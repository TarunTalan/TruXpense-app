# 📊 TruXpense

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-blue.svg)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-1.5.0-green.svg)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Material%203-Latest-orange.svg)](https://m3.material.io/)
[![Platform](https://img.shields.io/badge/Platform-Android-brightgreen.svg)](https://www.android.com/)

**TruXpense** is a sophisticated, AI-powered personal finance manager for Android. It goes beyond simple expense tracking by automatically parsing bank SMS, categorizing transactions, and providing actionable insights through a premium, Material 3-driven user interface.

---

## 🌟 Key Features

- **🤖 AI-Powered Automation**: Automatically detects and parses bank SMS notifications to record transactions in real-time.
- **📈 Advanced Analytics**: Visualize your spending habits with dynamic charts, category breakdowns, and daily spending trends.
- **💰 Smart Budgeting**: Set monthly budgets for specific categories and get notified as you approach your limits.
- **🎯 Savings Goals**: Create and track multiple savings goals with a dedicated distribution system.
- **📄 Professional Reports**: Generate detailed expense reports (PDF/Image) with visual charts, ready for sharing or record-keeping.
- **🔒 Report Vault**: A secure space to manage and view all your historical financial reports.
- **🌗 Full Theme Support**: Beautifully crafted Light and Dark modes that adapt to your system preferences.
- **🔔 Smart Reminders**: Customizable notifications to keep you on track with your financial goals.

---

## 📸 App Showcase

### 🏠 Core Experience & Dashboard
| Dashboard (Light) | Dashboard (Dark) | Scrolled Overview |
| :---: | :---: | :---: |
| ![Dashboard](assets/screenshots/dashboard.jpg) | ![Dashboard Dark](assets/screenshots/dashboard_dark.jpg) | ![Dashboard Scrolled](assets/screenshots/dashboard_scrolled.jpg) |

| Smart SMS Parsing | Budget Overview Card | Recent Transactions |
| :---: | :---: | :---: |
| ![SMS Parsing](assets/screenshots/notification_popup_dark.jpg) | ![Budget Card](assets/screenshots/dashboard_budgets_card.jpg) | ![Transactions](assets/screenshots/recent_transactions.jpg) |

### 📊 Analytics & Deep Insights
| Spending Trends | Category Analytics | Daily Spending Chart |
| :---: | :---: | :---: |
| ![Analytics](assets/screenshots/analytics.jpg) | ![Analytics Dark](assets/screenshots/anlytics_dark.jpg) | ![Daily Chart](assets/screenshots/daily_spending_chart.jpg) |

| Spending Charts (Light) | Spending Charts (Dark) | Analytics Bottom Sheet |
| :---: | :---: | :---: |
| ![Charts](assets/screenshots/analytics_spending_chart.jpg) | ![Charts Dark](assets/screenshots/analytics_spending_chart_dark.jpg) | ![Analytics Bottom](assets/screenshots/analytics_bottom_dark.jpg) |

### 💸 Budgeting & Financial Planning
| All Budgets | Budget Detail | Budget Actions |
| :---: | :---: | :---: |
| ![Budgets](assets/screenshots/budgets.jpg) | ![Budget Detail](assets/screenshots/budget_detail.jpg) | ![Budget Actions](assets/screenshots/budget_detail_actions.jpg) |

| Add New Budget | Add Budget (Dark) | Filter Transactions |
| :---: | :---: | :---: |
| ![Add Budget](assets/screenshots/add_budget.jpg) | ![Add Budget Dark](assets/screenshots/add_budget_dark.jpg) | ![Filter](assets/screenshots/filter_transactions.jpg) |

### 🎯 Savings & Goals
| Savings Overview | Goal Distribution | Savings Distribution |
| :---: | :---: | :---: |
| ![Savings](assets/screenshots/savings.jpg) | ![Goal Distribution](assets/screenshots/distribute_goal.jpg) | ![Savings Distribution](assets/screenshots/distribute_savings_dark.jpg) |

| Goal Details | Goal Progress | Create New Goal |
| :---: | :---: | :---: |
| ![Goal Detail](assets/screenshots/goal_detail.jpg) | ![Goal Bottom](assets/screenshots/goal_detail_bottom.jpg) | ![Create Goal](assets/screenshots/create_goal.jpg) |

### 📄 Professional Reporting
| Report Generation | Generated Report | Expense Summary |
| :---: | :---: | :---: |
| ![Create Report](assets/screenshots/create_report.jpg) | ![Generated Report](assets/screenshots/generated_report.jpg) | ![Expense Report](assets/screenshots/expense_report.jpg) |

| Report Detail | Report Actions | Report Vault |
| :---: | :---: | :---: |
| ![Report Detail](assets/screenshots/report_detail_bottom.jpg) | ![Report Actions](assets/screenshots/report_actions.jpg) | ![Vault](assets/screenshots/Report_Vault.jpg) |

### ➕ Transactions & Data Entry
| Add Expense | Add Income | Add Transaction (Dark) |
| :---: | :---: | :---: |
| ![Add Expense](assets/screenshots/add-expense.jpg) | ![Add Income](assets/screenshots/add_income_dark.jpg) | ![Add Trans Dark](assets/screenshots/add_transaction_dark.jpg) |

### ⚙️ Settings & Customization
| User Settings | Notifications | Preferences |
| :---: | :---: | :---: |
| ![Settings](assets/screenshots/settings.jpg) | ![Notifications](assets/screenshots/notifiaction_and_reminder_settings.jpg) | ![Preferences](assets/screenshots/settings_preferences.jpg) |

### 👋 Onboarding & Authentication
| Onboarding 1 | Onboarding 2 | Onboarding 3 |
| :---: | :---: | :---: |
| ![Onboarding 1](assets/screenshots/onboarding_1.jpg) | ![Onboarding 2](assets/screenshots/onboarding_2.jpg) | ![Onboarding 3](assets/screenshots/onboarding_3.jpg) |

| Login Screen | Signup Screen | Auth Dark Mode |
| :---: | :---: | :---: |
| ![Login](assets/screenshots/login.jpg) | ![Signup](assets/screenshots/signup.jpg) | ![Notification Dark](assets/screenshots/notification_dark.jpg) |

---

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Modern Declarative UI)
- **Design System**: Material Design 3 (M3)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Local Database**: Room Persistence Library
- **SMS Parsing**: Android SMS API + Custom Regex/AI logic
- **Charts**: MPAndroidChart / Compose Charts
- **Dependency Injection**: Hilt / Dagger
- **Background Tasks**: WorkManager

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug (or later)
- Android SDK 24+
- A physical device or emulator with SMS capabilities (for testing auto-parsing)

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/TarunTalan/TruXpense-app.git
   ```
2. Open the project in Android Studio.
3. Sync the Gradle files.
4. Run the app on your device/emulator.

---

## 🤝 Contributing

Contributions are what make the open-source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📧 Contact

**Tarun Talan** - [Your LinkedIn/Email]

Project Link: [https://github.com/TarunTalan/TruXpense-app](https://github.com/TarunTalan/TruXpense-app)

---
*Developed with ❤️ by Tarun Talan*
