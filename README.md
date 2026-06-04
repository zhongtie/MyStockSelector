# MyStockSelector

基于中证1000成分股的基本面选股与组合模拟 Android 应用。

## 项目目标

1. **基本面条件选股**：以 ROE/PE 比值排序，结合股息率过滤，从中证1000成分股中筛选标的。
2. **组合模拟**：创建投资组合，支持自定义权重建仓，并跟踪组合收益。（回测功能后续迭代）
3. **数据本地化**：成分股与财务数据均存储在设备本地，支持离线查阅与历史回溯。

## 技术方案概览

本项目采用 **纯 Android 本地应用** 架构，不依赖 Python 后端或 akshare。

| 数据类型 | 来源 | 说明 |
|----------|------|------|
| 中证1000成分股 | CSV 文件导入 | 指数每年约调整一次，手动维护即可 |
| 财务指标（PE、ROE、股息率） | 东方财富 JSON API | 不解析 HTML 页面，调用底层数据接口 |
| 行情数据 | 东方财富 JSON API | 用于组合收益跟踪 |
| 本地存储 | Room 数据库 | 成分股、财务快照、组合、采集日志 |

### 架构图

```
┌─────────────────────────────────────────────────────────┐
│                    Android App                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌─────────┐ │
│  │ Excel导入 │  │ 数据采集  │  │ 选股引擎  │  │ 组合管理 │ │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬────┘ │
│       │             │             │             │       │
│       └─────────────┴──────┬──────┴─────────────┘       │
│                            │                            │
│                     ┌──────┴──────┐                     │
│                     │  Room 数据库 │                     │
│                     └──────┬──────┘                     │
│                            │                            │
│                     ┌──────┴──────┐                     │
│                     │ Compose UI  │                     │
│                     └─────────────┘                     │
└──────────────────────────┬──────────────────────────────┘
                           │ HTTPS
                    ┌──────┴──────┐
                    │ 东方财富 API │
                    └─────────────┘
```

## 选股逻辑

```
选股池 = 中证1000 成分股（指定交易日快照）

过滤条件：
  1. PE > 0（排除亏损、无效 PE）
  2. ROE 有效
  3. 股息率 >= 用户设定阈值（默认 3%，可配置）
  4. 行业（可选，用户指定一个或多个行业；不选则全池）

排序：
  score = ROE / PE

输出：
  取 score 排名前 N 只（N 由用户设定，如 10 / 20 / 50）
```

**ROE/PE** 表示单位估值下的盈利能力，兼顾盈利质量与估值水平。

## 数据库表设计

### index_constituent（成分股）

| 字段 | 类型 | 说明 |
|------|------|------|
| stock_code | TEXT PK | 6 位股票代码，如 `688498` |
| stock_name | TEXT | 股票名称 |
| market | TEXT | 市场：`SH` / `SZ` |
| industry | TEXT | 所属行业（采集或导入时填充，供选股筛选） |
| effective_date | TEXT | 成分生效日期 |
| is_current | INTEGER | 是否当前有效成分（0/1） |

### financial_snapshot（财务快照）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER PK | 自增主键 |
| stock_code | TEXT | 股票代码 |
| trade_date | TEXT | 采集日期（YYYY-MM-DD） |
| pe | REAL | 市盈率 |
| roe | REAL | 净资产收益率（%） |
| dividend_yield | REAL | 股息率（%） |
| roe_pe_ratio | REAL | ROE / PE，预计算 |
| report_period | TEXT | 财务报告期 |
| created_at | TEXT | 记录创建时间 |

### daily_quote（日线行情）

| 字段 | 类型 | 说明 |
|------|------|------|
| stock_code | TEXT | 股票代码 |
| trade_date | TEXT | 交易日期 |
| close | REAL | 收盘价 |
| pct_change | REAL | 涨跌幅（%） |

### portfolio（投资组合）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER PK | 自增主键 |
| name | TEXT | 组合名称 |
| created_at | TEXT | 创建时间 |
| initial_capital | REAL | 初始资金 |

### portfolio_holding（组合持仓）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER PK | 自增主键 |
| portfolio_id | INTEGER | 所属组合 |
| stock_code | TEXT | 股票代码 |
| shares | REAL | 持仓数量 |
| cost_price | REAL | 成本价 |
| weight | REAL | 目标权重（%，用户自定义，合计应为 100） |
| buy_date | TEXT | 买入日期 |

### sync_log（采集任务日志）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER PK | 自增主键 |
| task_type | TEXT | 任务类型：`constituent` / `financial` / `quote` |
| started_at | TEXT | 开始时间 |
| finished_at | TEXT | 结束时间 |
| total_count | INTEGER | 总数量 |
| success_count | INTEGER | 成功数 |
| fail_count | INTEGER | 失败数 |
| fail_list | TEXT | 失败股票代码列表（JSON） |

## 成分股 CSV 文件格式

当前仅支持 `.csv` 导入（`.xlsx` 暂不实现）。表头示例：

```csv
股票代码,股票名称,生效日期
688498,源杰科技,2025-12-15
000001,平安银行,2025-12-15
```

- **股票代码**：6 位数字，不含市场前缀
- **股票名称**：可选
- **生效日期**：本次成分调整生效日（YYYY-MM-DD）

每年中证1000成分调整后，导出新的 CSV 文件并在 App 内重新导入即可。

## 东方财富数据接口

> 参考页面：[东方财富个股数据](https://data.eastmoney.com/stockdata/688498.html)  
> 实现时调用 JSON API，不解析 HTML。

### 股票代码与市场映射

```
代码前缀 → 市场 → secid 格式
  6xxxxx / 688xxx → 上海(SH) → 1.{code}
  0xxxxx / 3xxxxx → 深圳(SZ) → 0.{code}
```

### 主要数据字段

| 策略字段 | 接口来源 | 备注 |
|----------|----------|------|
| PE（市盈率） | 行情接口 | 亏损股 PE 可能 ≤ 0，需过滤 |
| ROE（%） | 财务指标接口 | 注意报告期（季报/年报） |
| 股息率（%） | 行情或分红接口 | 统一 TTM 口径 |
| 行业 | 行情或基础信息接口 | 写入 `index_constituent.industry`，供选股筛选 |
| 收盘价 | 行情接口 | 组合收益计算 |

### 采集注意事项

- 中证1000 约 1000 只股票，全量采集 **限速 300ms/只**
- 单股失败 **最多重试 3 次**，仍失败则记入 `sync_log.fail_list`
- **增量采集**：再次触发时，跳过当日（或当前批次）已成功写入 `financial_snapshot` 的股票，避免重复请求
- 接口非官方公开文档，可能变更；统一封装在 `EastMoneyDataSource`，便于维护
- 每次采集结果写入 `financial_snapshot`，支撑历史快照查阅
- 采集方式：**仅手动触发**（不设定时任务）
- 数据来源为东方财富，仅供个人学习研究，请控制采集频率

## 实施阶段

### P0 — 基础数据层

- [ ] 搭建 Room 数据库与 Entity / DAO
- [ ] 实现 CSV 成分股导入
- [ ] 成分股列表展示页

### P1 — 东方财富 API 封装

- [ ] 封装 `EastMoneyDataSource`（secid 转换、HTTP 请求）
- [ ] 单股 PE / ROE / 股息率拉取
- [ ] 写入 `financial_snapshot`

### P2 — 批量采集

- [ ] 成分股全量财务数据采集（带进度条）
- [ ] 限速 300ms/只、失败重试 3 次、增量跳过已采集股票
- [ ] 采集日志（`sync_log`）与数据同步状态页

### P3 — 选股引擎

- [ ] 股息率阈值过滤（默认 3%，可配置）
- [ ] 行业筛选（可选一个或多个行业）
- [ ] 按 ROE/PE 排序，输出数量可配置
- [ ] 选股结果列表页
- [ ] 支持选择历史快照日期

### P4 — 组合管理

- [ ] 创建 / 编辑投资组合
- [ ] 持仓录入（代码、数量、成本价、自定义权重）
- [ ] 拉取最新价，计算单股盈亏与组合总收益

### P5 — 回测与完善（暂缓）

> 当前版本不实现回测功能，以下任务留待后续迭代。

- [ ] ~~按历史快照模拟建仓~~
- [ ] ~~区间收益计算与对比~~
- [ ] ~~组合净值曲线~~
- [ ] ~~定时采集（WorkManager）~~

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM |
| 本地数据库 | Room |
| 网络 | Retrofit + OkHttp |
| 导航 | Compose Navigation |
| 依赖注入 | Hilt |
| 构建 | Gradle (Kotlin DSL) + Version Catalog |
| minSdk | 24 |
| targetSdk | 36 |

### 待引入依赖（实施时添加）

```kotlin
// Room
implementation(libs.androidx.room.runtime)
implementation(libs.androidx.room.ktx)
ksp(libs.androidx.room.compiler)

// 网络
implementation(libs.retrofit)
implementation(libs.retrofit.converter.gson)
implementation(libs.okhttp)
implementation(libs.okhttp.logging)

// Navigation
implementation(libs.androidx.navigation.compose)

// ViewModel
implementation(libs.androidx.lifecycle.viewmodel.compose)

// Hilt
implementation(libs.hilt.android)
ksp(libs.hilt.compiler)
implementation(libs.androidx.hilt.navigation.compose)

// CSV 解析（标准库按行 split，或轻量 CSV 库）
```

## 项目结构（规划）

```
app/src/main/java/com/example/mystockselector/
├── MainActivity.kt
├── MyStockSelectorApp.kt
├── data/
│   ├── local/          # Room Entity、DAO、Database
│   ├── remote/         # EastMoneyDataSource、API 模型
│   └── repository/     # 数据仓库
├── domain/
│   ├── model/          # 业务模型
│   └── usecase/        # 选股、导入等用例
├── di/                 # Hilt 模块
├── ui/
│   ├── theme/
│   ├── navigation/
│   ├── import/         # 成分股导入
│   ├── sync/           # 数据同步
│   ├── screener/       # 选股
│   └── portfolio/      # 组合管理
└── (backtest/、worker/ 暂缓)
```

## 开发环境

### 要求

- Android Studio（推荐最新稳定版）
- JDK 11+
- Android SDK 36

### 克隆与构建

```bash
git clone <repository-url>
cd MyStockSelector

# 构建 Debug APK
./gradlew assembleDebug

# 运行单元测试
./gradlew test
```

Windows PowerShell：

```powershell
.\gradlew.bat assembleDebug
```

### 运行

1. 用 Android Studio 打开项目
2. 连接设备或启动模拟器（API 24+）
3. 点击 Run

## 协同开发说明

### 分支策略

| 分支 | 用途 |
|------|------|
| `main` | 稳定可构建版本 |
| `dev` | 日常开发集成分支 |
| `feature/*` | 功能分支，如 `feature/p0-room-database` |

### 提交规范

```
<type>(<scope>): <description>

type: feat | fix | docs | refactor | test | chore
scope: data | ui | screener | portfolio | sync | ...
```

示例：

```
feat(data): add Room entities for financial snapshot
feat(sync): implement East Money API data source
feat(ui): add CSV constituent import screen
```

### 进度同步

- 完成某个阶段任务后，更新本文档对应阶段的 `[ ]` → `[x]`
- 重要设计变更同步更新「数据库表设计」「选股逻辑」等章节
- 接口变更记录在 `data/remote/` 相关代码注释或 CHANGELOG

### 注意事项

- **不要提交** `local.properties`、签名文件、`.idea/` 个人配置
- `app/build/` 已在 `.gitignore` 中
- 成分股 CSV 样例可放在 `sample_data/` 目录（不含敏感信息）
- 东方财富接口调用逻辑集中在 `EastMoneyDataSource`，避免散落

## 已确认决策（2026-06-04）

| 事项 | 决定 |
|------|------|
| 股息率默认阈值 | **3%**（App 内可配置） |
| 选股输出 | 用户可选 **行业** + **数量** |
| 组合建仓方式 | **自定义权重**（非等权） |
| 股息率口径 | **TTM** |
| 财务数据采集 | **仅手动触发** |
| 依赖注入 | **Hilt** |
| 成分股导入格式 | **仅 CSV** |
| 批量采集限速 | **300ms/只** |
| 失败重试 | **3 次**，失败记入 `sync_log`，后续采集 **跳过已成功股票** |
| 回测功能 | **暂不实现**（P5 暂缓） |

## 许可证

待定（个人学习研究项目）

## 更新日志

| 日期 | 内容 |
|------|------|
| 2026-06-04 | 确认首期实施决策：股息率 3%、行业/数量可选、自定义权重、Hilt、CSV 导入、增量采集、暂缓回测 |
| 2026-05-29 | 确定技术方案：Excel 导入 + 东方财富 API + Room 本地存储；创建 README |
