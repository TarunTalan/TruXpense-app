# TruXpense

> **AI-powered Android expense tracker** — automatically parses bank SMS, categorises spending, manages budgets, and (coming soon) tracks income & savings so users always know where their money goes.

---

## Table of Contents

1. [Tech Stack](#tech-stack)
2. [Project Structure](#project-structure)
3. [Complete Screen Inventory](#complete-screen-inventory)
4. [Full User Flow](#full-user-flow)
   - [Auth Flow](#1-auth-flow)
   - [Onboarding Flow](#2-onboarding-flow)
   - [Home Tab](#3-home-tab)
   - [Transactions Tab](#4-transactions-tab)
   - [Budget Tab](#5-budget-tab)
   - [Analytics Tab](#6-analytics-tab)
   - [Settings Tab](#7-settings-tab)
   - [Notifications](#8-notifications)
   - [SMS Auto-Parsing](#9-sms-auto-parsing)
5. [Income & Savings Tracking — Planned Integration](#income--savings-tracking--planned-integration)
   - [What Changes Where](#what-changes-where)
   - [New Screen: Income Setup](#new-screen-income-setup)
   - [Updated: Home Tab](#updated-home-tab)
   - [Updated: Analytics Tab](#updated-analytics-tab)
   - [Updated: Settings Tab](#updated-settings-tab)
   - [Updated: Add Budget Screen](#updated-add-budget-screen)
   - [Full Updated User Flow](#full-updated-user-flow-with-income--savings)
6. [Notification Deep-Links](#notification-deep-links)
7. [Navigation Map](#navigation-map)
8. [Data Layer Summary](#data-layer-summary)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Hilt DI |
| Local DB | Room (budgets, expenses, pending SMS transactions) |
| Preferences | DataStore Preferences (auth, currency, notification settings) |
| Navigation | Compose Navigation (single-activity) |
| Background work | WorkManager (budget checker, daily reminder, monthly reset) |
| Push notifications | Firebase Cloud Messaging (FCM) |
| Auth | Email + OTP, Google Sign-In |
| SMS parsing | `SmsParserEngine` + ML categoriser (`MlCategorizer`) |
| Network | Retrofit + OkHttp |

---

## Project Structure

```
app/src/main/java/com/example/truxpense/
├── data/
│   ├── local/
│   │   ├── dao/            BudgetDao, ExpenseDao, PendingTransactionDao
│   │   ├── database/       AppDatabase, SmsDatabase
│   │   ├── datastore/      AuthPreferences, notificationDataStore
│   │   └── entity/         BudgetEntity, ExpenseEntity, PendingTransactionEntity
│   ├── remote/             API services, authenticator
│   ├── repository/
│   │   ├── auth/           AuthRepository, GoogleSignInRepository
│   │   ├── budget/         BudgetRepository
│   │   ├── expense/        ExpenseRepository
│   │   ├── notification/   NotificationRepository
│   │   ├── onboarding/     OnboardingRepository
│   │   └── sms/            PendingTransactionRepository
│   ├── session/            AuthSessionManager, TokenManager, TokenRefresher
│   └── sms/                SmsPermissionHelper, parser models, SmsParserEngine
├── di/                     Hilt modules (App, Network, Notification, SMS, WorkManager)
├── notification/
│   ├── channels/           NotificationChannels, NotificationConstants
│   ├── datastore/          NotificationSettings DataStore
│   ├── deeplink/           NotificationDeepLinkManager, sealed NotificationDeepLink
│   ├── helper/             NotificationHelper (builds + posts all notifications)
│   ├── scheduler/          NotificationScheduler (WorkManager wiring)
│   └── workers/            BudgetThresholdWorker, DailyExpenseReminderWorker, MonthlyBudgetResetWorker
├── presentation/
│   ├── navigation/         Screen, AppNavHost, BottomNavBarMenu, safeNavigate
│   ├── screens/
│   │   ├── auth/           Intro, Login, Signup, OTP screens + AuthFlowViewModel
│   │   ├── onboarding/     Username, Currency, SmsPermission, Loading screens
│   │   ├── splash/         SplashScreen
│   │   └── dashboard/
│   │       ├── addexpense/ AddExpenseScreen + VM
│   │       ├── analytics/  AnalyticsScreen + VM
│   │       ├── budget/     BudgetTab, AddBudget, BudgetDetail screens + VMs
│   │       ├── components/ Shared Compose components
│   │       ├── home/       HomeTabScreen, HomeScreen (shell) + VM
│   │       ├── notifications/ NotificationScreen + VMs
│   │       ├── settings/   Settings, PersonalInfo, Security, Notifications,
│   │       │               LinkedAccounts, Help, Policy, About, DeleteAccount
│   │       ├── sms/        PendingTransactionsScreen + VM
│   │       └── transaction/ TransactionScreen, Detail, Edit + VMs
│   ├── theme/              TruXpenseTheme, DashboardDimens
│   └── utils/              currencyFormat, formatAmountParts, toCurrency, UiUtils
├── service/                TruxpenseFirebaseMessagingService, TransactionNotifier
└── sms/                    SmsBroadcastReceiver, ConfirmActionReceiver,
                            HistoryRescanWorker, TransactionSyncWorker
```

---

## Complete Screen Inventory

### Auth & Onboarding

| Screen | Route | Purpose |
|---|---|---|
| `SplashScreen` | `splash` | App entry — checks auth state, routes to Intro or Dashboard |
| `IntroScreen` | `intro` | Landing page with Login / Sign Up options |
| `LoginScreen` | `login` | Email + password login |
| `SignupScreen` | `signup` | New account creation |
| `OtpScreen` | `otp` | OTP verification for signup / password reset |
| `UsernameScreen` | `username` | Onboarding step 1 — set display name |
| `CurrencyScreen` | `currency` | Onboarding step 2 — pick preferred currency |
| `SmsPermissionScreen` | `sms_permission` | Onboarding step 3 — request SMS read permission |
| `LoadingScreen` | `loading` | Post-onboarding loading / data sync |

### Dashboard (Bottom Nav)

| Screen | Route | Purpose |
|---|---|---|
| `HomeTabScreen` | `home` | Monthly spend, budget summary, top categories, recent transactions |
| `TransactionsScreen` | `transactions` | Full paginated transaction list with category filter |
| `BudgetTab` | `budget` | All category budgets with spend progress bars |
| `AnalyticsScreen` | `analytics` | Donut chart, trend chart, period selector, top merchant/category |
| `SettingsScreen` | `settings` | User profile card + all settings navigation |

### Dashboard (Sub-screens)

| Screen | Route | Purpose |
|---|---|---|
| `AddExpenseScreen` | `home/add_expense` | Log a new expense manually |
| `TransactionDetailScreen` | `transactions/detail/{id}` | Full detail of a single transaction |
| `EditExpenseScreen` | `transactions/edit/{id}` | Edit an existing transaction |
| `AddBudgetScreen` | `budget/add` | Create a new category budget (duplicate guard) |
| `BudgetDetailScreen` | `budget/detail/{name}/{limit}/{spent}` | Category budget with transaction list |
| `NotificationScreen` | `notifications` | In-app notification centre with mark-read / deep-link |
| `PendingTransactionsScreen` | `sms/pending_review` | Review SMS-parsed transactions before confirming |

### Settings Sub-screens

| Screen | Route | Purpose |
|---|---|---|
| `PersonalInfoScreen` | `settings/personal_info` | View / edit name, email, phone |
| `LinkedAccountsScreen` | `settings/linked_accounts` | Bank / SMS source status |
| `SecurityScreen` | `settings/security` | Password, login method |
| `NotificationsScreen` | `settings/notifications` | Notification toggles + budget alert thresholds |
| `HelpScreen` | `settings/help` | FAQs and support |
| `PolicyScreen` | `settings/privacy_policy` | Privacy policy |
| `TermsScreen` | `settings/terms` | Terms of service |
| `AboutScreen` | `settings/about` | App version, licences |
| `DeleteAccountScreen` | `settings/delete_account` | Permanent account deletion |

---

## Full User Flow

### 1. Auth Flow

```
App launch
    │
    ▼
SplashScreen ──── auth token valid? ──── Yes ──▶ DashboardScreen
    │
    No
    ▼
IntroScreen
    ├── "Log In"   ──▶ LoginScreen ──▶ (success) ──▶ DashboardScreen
    └── "Sign Up"  ──▶ SignupScreen
                            │
                            ▼
                       OtpScreen (email verification)
                            │
                            ▼
                       Onboarding Flow
```

**LoginScreen**
- Email + password fields
- "Forgot password?" link → OTP reset flow
- Google Sign-In button → Google OAuth → Dashboard
- Error states: wrong credentials, network error

**SignupScreen**
- Name, email, password, confirm password
- Real-time validation (password strength, email format)
- On submit → OtpScreen

**OtpScreen**
- 6-digit OTP input with countdown resend timer
- Auto-advance to next digit
- On verified → Onboarding or Dashboard

---

### 2. Onboarding Flow

```
UsernameScreen ──▶ CurrencyScreen ──▶ SmsPermissionScreen ──▶ LoadingScreen ──▶ Dashboard
```

**UsernameScreen**
- Single text field — display name
- Back press exits app (no back navigation in onboarding)

**CurrencyScreen**
- Searchable list of all world currencies
- Selected currency used for all amount formatting throughout the app

**SmsPermissionScreen**
- Explains SMS auto-parsing benefit
- "Allow" → requests `RECEIVE_SMS` + `READ_SMS` runtime permissions
- "Skip" → proceeds without SMS; banner shown on Home later

**LoadingScreen**
- Syncs initial data (budgets, transactions) from backend
- Animated progress indicator

---

### 3. Home Tab

**Entry point:** `HomeTabScreen`

```
HomeTabScreen
    │
    ├── [Empty state] ─────────────────────────── No transactions yet
    │       └── "Add your first expense" CTA
    │
    └── [Loaded state]
            │
            ├── TopBar
            │     ├── 🔔 Notification icon (badge = unread count)
            │     │       └── tap ──▶ NotificationScreen
            │     └── 👤 Profile icon
            │               └── tap ──▶ PersonalInfoScreen
            │
            ├── SMS Permission banner (shown if permission not granted)
            │       └── "Allow" ──▶ runtime permission dialog
            │
            ├── Pending SMS banner (shown if parsed SMS awaiting review)
            │       └── tap ──▶ PendingTransactionsScreen
            │
            ├── "Spend this month" card
            │       └── Large amount display (currency-formatted)
            │
            ├── "Budget" summary card (only when budgets exist)
            │       ├── Remaining / Total budget
            │       ├── Animated progress bar
            │       └── "Details" button ──▶ BudgetTab
            │
            ├── Insight nudge card
            │       └── Contextual spending tip
            │
            ├── "Highest spending categories" section
            │       └── SpendingCategoryCard list (progress bars, amounts)
            │
            ├── "Recent transactions" card
            │       ├── Last 4 transactions
            │       └── "View all" ──▶ TransactionsScreen
            │
            └── FAB (+) ──▶ AddExpenseScreen
```

**AddExpenseScreen**
- Amount field (numeric, currency prefix)
- Category picker (dropdown with icons)
- Merchant / description field
- Date picker (defaults to today)
- Payment method selector
- "Save" → adds to Room + triggers budget threshold check

---

### 4. Transactions Tab

```
TransactionsScreen
    ├── Search bar
    ├── Category filter chips (All, Food, Transport, …)
    ├── Sorted paginated list of all transactions
    │     └── tap item ──▶ TransactionDetailScreen
    │                           ├── Full detail view
    │                           ├── Edit button ──▶ EditExpenseScreen
    │                           └── Delete button (with confirmation dialog)
    └── FAB (+) ──▶ AddExpenseScreen
```

**EditExpenseScreen**
- Pre-filled with existing transaction data
- Same fields as AddExpenseScreen
- "Save" → updates Room record

---

### 5. Budget Tab

```
BudgetTab
    │
    ├── [Empty state] — "No budgets yet" + "Add Budget" button
    │
    └── [Loaded state]
            ├── Month navigator (← March 2026 →)
            ├── Total budget / total spent summary
            ├── Category budget cards (each with progress bar + colour coding)
            │     └── tap card ──▶ BudgetDetailScreen
            │                         ├── Category name + limit
            │                         ├── Progress ring
            │                         ├── Transaction list for this category
            │                         ├── "See all" ──▶ TransactionsScreen (pre-filtered)
            │                         └── Delete budget option
            └── FAB (+) ──▶ AddBudgetScreen
                                ├── Category dropdown (only shows categories
                                │   without an existing budget — duplicate guard)
                                ├── Monthly limit amount field
                                ├── Inline error if duplicate selected
                                └── "Create budget" button (disabled until valid)
```

**Progress bar colour coding**

| Usage | Colour |
|---|---|
| < 70% | Primary (green/teal) |
| 70–89% | Amber |
| ≥ 90% | Error (red) |

---

### 6. Analytics Tab

```
AnalyticsScreen
    │
    ├── Period selector — Week / Month / Year
    ├── Period navigator (← Feb 2026 →)
    │
    ├── Summary row
    │     ├── Total spent
    │     ├── Total budget
    │     └── % change vs previous period (▲ / ▼)
    │
    ├── Donut chart — category breakdown (animated on entry)
    │     └── tap segment → tooltip with category + amount
    │
    ├── Category legend list
    │     └── name, amount, colour swatch
    │
    ├── Trend chart — spend over time (animated on scroll into view)
    │     └── press-and-hold → tooltip with exact date + amount
    │
    └── Insights cards
          ├── Top merchant this period
          └── Top spending category
```

---

### 7. Settings Tab

```
SettingsScreen
    │
    ├── Profile header card
    │     ├── Avatar (initials-based, coloured)
    │     ├── Display name
    │     ├── Phone number
    │     └── "Edit Profile" button ──▶ PersonalInfoScreen
    │
    ├── Account section
    │     ├── Linked accounts ──▶ LinkedAccountsScreen
    │     └── Security ──▶ SecurityScreen
    │
    ├── Preferences section
    │     └── Notifications & Reminders ──▶ NotificationsScreen
    │                                           ├── Spending Alerts section
    │                                           │     ├── Budget Alerts toggle
    │                                           │     │     └── [expanded] Alert trigger
    │                                           │     │           ├── Segmented: "% of budget" / "Fixed amount ₹"
    │                                           │     │           ├── % mode: slider 50–100%
    │                                           │     │           └── Fixed mode: ₹ amount input + error validation
    │                                           │     ├── Spending Insights toggle
    │                                           │     └── Unusual Spending toggle
    │                                           └── Daily Expense Reminder section
    │                                                 ├── Reminder toggle
    │                                                 └── [expanded] Time picker (dial + keyboard)
    │
    ├── Support section
    │     ├── Help & FAQs ──▶ HelpScreen
    │     ├── Privacy Policy ──▶ PolicyScreen
    │     ├── Terms of Service ──▶ TermsScreen
    │     └── About TruXpense ──▶ AboutScreen
    │
    ├── Log Out (with confirmation dialog)
    └── Delete Account (destructive — red text) ──▶ DeleteAccountScreen
```

---

### 8. Notifications

**In-app NotificationScreen**
```
NotificationScreen
    ├── "Mark all as read" action
    ├── Grouped by time label (Today, Yesterday, This Week, …)
    └── Notification item (long-press to mark individual)
          ├── Icon (type-based: warning, sync, lightbulb, trending, bell)
          ├── Title + body
          ├── Time label
          ├── Unread dot indicator
          └── tap ──▶ destination screen (budget detail / analytics / transactions)
```

**Push Notifications (FCM + WorkManager)**

| Trigger | Title | Destination on tap |
|---|---|---|
| Budget ≥ threshold% | "Food budget at 91%" | BudgetDetailScreen (Food) |
| Budget exceeded 100% | "Food budget exceeded" | BudgetDetailScreen (Food) |
| Daily reminder | "Log today's expenses" | AddExpenseScreen |
| Monthly reset | "New month — review your budgets" | BudgetTab |
| Monthly summary | "March spending summary" | AnalyticsScreen |

---

### 9. SMS Auto-Parsing

```
Bank sends SMS
    │
    ▼
SmsBroadcastReceiver (RECEIVE_SMS)
    │
    ▼
SmsParserEngine — regex + MlCategorizer
    │
    ▼
PendingTransactionEntity saved to Room
    │
    ▼
TransactionNotifier — posts in-app notification
    │
    ▼
User taps pending banner on HomeTabScreen
    │
    ▼
PendingTransactionsScreen
    ├── Review parsed amount, merchant, category
    ├── "Confirm" ──▶ added to ExpenseRepository
    └── "Reject" ──▶ deleted from pending
```

---

## Income & Savings Tracking — Planned Integration

> **Status:** Roadmapped — not yet implemented.  
> All changes are **additive** and **backward-compatible** (default income = 0, all new UI hidden until income is set).

---

### What Changes Where

| File | Type | Change |
|---|---|---|
| `data/local/datastore/AuthPreferences.kt` | Modify | Add `MONTHLY_INCOME` DataStore key, `monthlyIncome: Flow<Double>`, `setMonthlyIncome()` |
| `presentation/screens/dashboard/home/HomeViewModel.kt` | Modify | Add `monthlyIncome`, `netBalance`, `savingsRate`, `isIncomeSet` StateFlows |
| `presentation/screens/dashboard/analytics/AnalyticsViewModel.kt` | Modify | Add `monthlyIncome`, `netBalance`, `savingsRate` to `AnalyticsUiState` |
| `presentation/screens/dashboard/settings/SettingsViewModel.kt` | Modify | Add income getter + setter |
| `presentation/screens/dashboard/home/HomeTabScreen.kt` | Modify | Add Net Balance card, conditional income prompt chip |
| `presentation/screens/dashboard/analytics/AnalyticsScreen.kt` | Modify | Add Savings Rate card after summary row |
| `presentation/screens/dashboard/settings/SettingsScreen.kt` | Modify | Add "Monthly Income" row in Preferences section |
| `presentation/screens/dashboard/budget/AddBudgetScreen.kt` | Modify | Add "Suggested: ₹X" chip under amount field |
| `presentation/navigation/Screen.kt` | Modify | Add `Settings.Income = "settings/income"` route |
| `presentation/screens/dashboard/home/HomeScreen.kt` | Modify | Register `IncomeSetupScreen` composable route |
| `presentation/screens/dashboard/settings/IncomeSetupScreen.kt` | **New** | Income entry screen |

---

### New Screen: Income Setup

**Route:** `settings/income`  
**ViewModel:** `SettingsViewModel`

```
┌─────────────────────────────────────────────┐
│  ←  Monthly Income                          │
├─────────────────────────────────────────────┤
│                                             │
│  Your monthly take-home income              │
│                                             │
│  ┌─────────────────────────────────────┐   │
│  │  ₹   45,000                        │   │
│  └─────────────────────────────────────┘   │
│                                             │
│  Used to calculate your savings rate and   │
│  suggest budget limits.                     │
│                                             │
│  [          Save Income           ]         │
└─────────────────────────────────────────────┘
```

**Behaviour:**
- Pre-fills with current saved income (0 shows as empty placeholder)
- Validates: amount must be > 0
- On save → writes to `AuthPreferences.MONTHLY_INCOME` via DataStore
- `onBack` / `onSave` both pop back stack

---

### Updated: Home Tab

New **Net Balance card** inserted after the existing "Spend this month" card.  
Hidden entirely when `isIncomeSet == false`.

```
┌─────────────────────────────────────────────┐
│  This month                                 │
│                                             │
│   Income        Spent         Balance       │
│   ₹45,000      ₹28,400       ₹16,600       │
│                                             │
│  ━━━━━━━━━━━━━━━━━━━━━░░░░░░░░  63%        │
│  spent of income                            │
└─────────────────────────────────────────────┘
```

**Progress bar colour logic:**

| Spend % of income | Bar colour |
|---|---|
| < 70% | Green (primary) |
| 70–89% | Amber |
| ≥ 90% | Red (error) |

**Income not set state** — instead of the card, a subtle chip is shown:

```
  💡 Set your monthly income to track savings  →
```
Tapping the chip navigates to `IncomeSetupScreen`.

**Updated Home Tab flow:**
```
HomeTabScreen (loaded state)
    ├── TopBar (unchanged)
    ├── SMS banners (unchanged)
    ├── "Spend this month" card (unchanged)
    ├── Net Balance card  ◄─── NEW (hidden until income set)
    │     ├── Income / Spent / Balance columns
    │     ├── Colour-coded progress bar
    │     └── "Set income" chip (when income not set)
    ├── "Budget" summary card (unchanged)
    ├── Insight nudge (unchanged)
    ├── Top categories (unchanged)
    ├── Recent transactions (unchanged)
    └── FAB (unchanged)
```

---

### Updated: Analytics Tab

New **Savings Rate card** added between the Summary row and the Donut chart.  
Hidden when `monthlyIncome == 0`.

```
┌─────────────────────────────────────────────┐
│  Savings this month                         │
│                                             │
│        ₹16,600 saved  ·  37%               │
│                                             │
│  ━━━━━━━━━━━━━━━░░░░░░░░░░░░░░░░           │
│  Income ₹45K              Spent ₹28.4K      │
└─────────────────────────────────────────────┘
```

**Updated Analytics flow:**
```
AnalyticsScreen
    ├── Period selector (unchanged)
    ├── Period navigator (unchanged)
    ├── Summary row (unchanged)
    ├── Savings Rate card  ◄─── NEW (hidden until income set)
    ├── Donut chart (unchanged)
    ├── Category legend (unchanged)
    ├── Trend chart (unchanged)
    └── Insights cards (unchanged)
```

---

### Updated: Settings Tab

New **Monthly Income** row added to the Preferences section.

```
Preferences section
    ├── Notifications & Reminders ──▶ NotificationsScreen
    └── Monthly Income   ₹45,000  ──▶ IncomeSetupScreen   ◄─── NEW
          "Tap to update"
```

When income is not set, the row shows `"Not set"` in place of the amount.

**Updated Settings flow:**
```
SettingsScreen
    ├── Profile header card (unchanged)
    ├── Account section (unchanged)
    ├── Preferences section
    │     ├── Notifications & Reminders (unchanged)
    │     └── Monthly Income  ◄─── NEW ──▶ IncomeSetupScreen
    ├── Support section (unchanged)
    ├── Log Out (unchanged)
    └── Delete Account (unchanged)
```

---

### Updated: Add Budget Screen

New **suggested limit chip** shown below the amount field when monthly income is set.

```
┌─────────────────────────────────────────────┐
│  Monthly Budget limit                       │
│                                             │
│  ₹  [  3,500                            ]  │
│                                             │
│  💡 Suggested: ₹6,750  (15% of income)     │  ◄── NEW (when income set)
│                                             │
│  This resets every month                    │
└─────────────────────────────────────────────┘
```

Tapping the chip fills the amount field with the suggested value.

**Suggestion percentages per category:**

| Category | % of income |
|---|---|
| Food | 15% |
| Transport | 10% |
| Bills | 20% |
| Shopping | 8% |
| Health | 5% |
| Entertainment | 5% |
| Groceries | 12% |
| Education | 8% |
| Travel | 7% |
| Other | 5% |

---

### Full Updated User Flow (with Income & Savings)

```
First launch
    └── Auth → Onboarding (Username → Currency → SMS Permission → Loading)
                    │
                    ▼
              DashboardScreen
                    │
          ┌─────────┼──────────────────┐
          │         │                  │
        Home    Analytics          Settings
          │         │                  │
          │    Savings Rate card   Monthly Income row
          │    (hidden until set)  (shows "Not set")
          │                            │
          │                    tap ──▶ IncomeSetupScreen
          │                            │   Enter ₹45,000
          │                            │   tap "Save Income"
          │                            ▼
          │                     Income stored in DataStore
          │                            │
          │            ┌───────────────┘
          │            │
          ▼            ▼
   Net Balance card   Savings Rate card
   now visible        now visible
   on Home            on Analytics
          │
          ├── "Set income" chip gone
          ├── Balance = ₹45,000 − ₹28,400 = ₹16,600
          └── Progress bar shows 63% (amber zone)
                    │
              tap "Details" ──▶ BudgetTab
                                    │
                              Add Budget ──▶ AddBudgetScreen
                                                │
                                        Suggested: ₹6,750
                                        for Food (15% of ₹45K)
```

---

## Notification Deep-Links

Every push/local notification carries an `EXTRA_DESTINATION` intent extra.  
`MainActivity.onNewIntent()` feeds it to `NotificationDeepLinkManager` which emits a  
`NotificationDeepLink` sealed class. `DashboardScreen` collects it and navigates.

| `EXTRA_DESTINATION` | Sealed class | Navigation result |
|---|---|---|
| `dest_add_expense` | `AddExpense` | Opens `AddExpenseScreen` |
| `dest_budget_detail` + category | `BudgetDetailByCategory` | Fetches live budget data → opens `BudgetDetailScreen` |
| `dest_budget_tab` | `BudgetTab` | Opens `BudgetTab` |
| `dest_analytics` | `Analytics` | Opens `AnalyticsScreen` |
| `dest_transactions` | `Transactions` | Opens `TransactionsScreen` |
| `dest_dashboard` | `Home` | Opens `HomeTabScreen` |

---

## Navigation Map

```
Splash
  └── Intro
        ├── Login ──────────────────────────────┐
        └── Signup ──▶ OTP ──▶ Username          │
                                  └── Currency   │
                                        └── SmsPermission
                                              └── Loading
                                                    └── Dashboard ◄────────────┘
                                                          │
                         ┌────────────┬─────────────┬────┴─────┬────────────┐
                        Home     Transactions     Budget    Analytics   Settings
                         │            │              │            │          │
                    AddExpense   Detail/Edit    AddBudget    (period   PersonalInfo
                    Notifications               BudgetDetail  tabs)    LinkedAccounts
                    PendingReview                              │        Security
                                                               │        Notifications
                                                               │        Income ◄── NEW
                                                               │        Help / Policy
                                                               │        Terms / About
                                                               └        DeleteAccount
```

---

## Data Layer Summary

### Room Tables

| Table | Entity | Key fields |
|---|---|---|
| `budgets` | `BudgetEntity` | id, category, amount, createdAt |
| `expenses` | `ExpenseEntity` | id, amount, category, merchant, timestamp, paymentMethod |
| `pending_transactions` | `PendingTransactionEntity` | id, rawSms, parsedAmount, parsedMerchant, parsedCategory, state |

### DataStore Keys (AuthPreferences)

| Key | Type | Purpose |
|---|---|---|
| `access_token` | String | JWT access token |
| `refresh_token` | String | JWT refresh token |
| `username` | String | Display name |
| `phone` | String | Phone number |
| `onboarding_complete` | Boolean | Onboarding gate |
| `monthly_income` | Double | **NEW** Monthly take-home income (0.0 = not set) |
| `income_updated_at` | Long | **NEW** Epoch ms of last income update |

### DataStore Keys (NotificationDataStore)

| Key | Purpose |
|---|---|
| `budget_threshold_enabled` | Budget alerts on/off |
| `threshold_percent` | Alert % (50–100) |
| `budget_alert_custom_limit_enabled` | Custom ₹ mode on/off |
| `budget_alert_custom_limit` | Custom ₹ threshold amount |
| `spending_insights_enabled` | Spending insights on/off |
| `unusual_spending_enabled` | Unusual spending on/off |
| `daily_reminder_enabled` | Daily reminder on/off |
| `daily_reminder_hour` | Reminder hour (0–23) |
| `daily_reminder_minute` | Reminder minute (0–59) |

---

*Last updated: March 3, 2026*
