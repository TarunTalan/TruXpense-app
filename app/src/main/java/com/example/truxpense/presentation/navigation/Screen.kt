package com.example.truxpense.presentation.navigation

object Screen {

    // ── Auth ──────────────────────────────────────────────────────────────────
    const val Splash = "splash"
    const val Login = "login"
    const val Intro = "intro"
    const val Signup = "signup"
    const val Otp = "otp"

    // ── Onboarding ────────────────────────────────────────────────────────────
    const val Username = "username"
    const val Currency = "currency"
    const val SmsPermission = "sms_permission"
    const val Loading = "loading"

    object Dashboard {

        /** Registered in the *outer* AppNavHost as the Home destination. */
        const val Root = "dashboard"

        // ── Home tab ─────────────────────────────────────────────────────────
        object Home {
            const val Root = "home"

            /**
             * Unified add-transaction screen (Expense / Income tabs).
             * Optional query param: ?tab=0 (expense) or ?tab=1 (income).
             * Replaces the old separate AddExpense / AddIncome routes.
             */
            const val AddTransaction = "home/add_transaction?tab={tab}"

            fun addTransactionRoute(tab: Int = 0) = "home/add_transaction?tab=$tab"

            /**
             * Backwards-compatible aliases for older code that referenced the
             * legacy AddExpense/AddIncome constants. These point to the unified
             * add-transaction routes with the appropriate tab query.
             */
            @Deprecated("Use addTransactionRoute(tab) instead")
            const val AddExpense = "home/add_transaction?tab=0"

            @Deprecated("Use addTransactionRoute(tab) instead")
            const val AddIncome = "home/add_transaction?tab=1"

            // ── Savings flow ─────────────────────────────────────────────────
            const val Savings = "home/savings"
            const val SavingsCreateGoal = "home/savings/create"
            const val SavingsEditGoal = "home/savings/edit/{goalId}"
            const val SavingsGoalDetail = "home/savings/detail/{goalId}"
            const val SavingsDistribute = "home/savings/distribute"
            const val AddSavings = "home/savings/add"
            const val SavingsGoalCompleted = "home/savings/goal_completed?goalName={goalName}&savedAmount={savedAmount}"

            fun goalCompletedRoute(goalName: String, savedAmount: Double): String {
                val encoded = java.net.URLEncoder.encode(goalName, "UTF-8")
                return "home/savings/goal_completed?goalName=$encoded&savedAmount=$savedAmount"
            }

            fun savingsEditRoute(goalId: Long) = "home/savings/edit/$goalId"
            fun savingsDetailRoute(goalId: Long) = "home/savings/detail/$goalId"
        }

        // ── Transactions tab ──────────────────────────────────────────────────
        object Transactions {
            const val Root = "transactions"

            /** Unified add-transaction screen reached from the Transactions tab. */
            const val AddTransaction = "transactions/add_transaction?tab={tab}"

            fun addTransactionRoute(tab: Int = 0) = "transactions/add_transaction?tab=$tab"

            @Deprecated("Use addTransactionRoute(tab) instead")
            const val AddExpense = "transactions/add_transaction?tab=0"

            const val Detail = "transactions/detail/{transactionId}"
            fun detailRoute(transactionId: String): String =
                "transactions/detail/${java.net.URLEncoder.encode(transactionId, "UTF-8")}"

            const val Edit = "transactions/edit/{transactionId}"
            fun editRoute(transactionId: String): String =
                "transactions/edit/${java.net.URLEncoder.encode(transactionId, "UTF-8")}"

            const val EditIncome = "transactions/edit_income/{incomeId}"
            fun editIncomeRoute(incomeId: String): String =
                "transactions/edit_income/${java.net.URLEncoder.encode(incomeId, "UTF-8")}"
        }

        // ── Budget tab ────────────────────────────────────────────────────────
        object Budget {
            const val Root = "budget"
            const val Add = "budget/add"

            const val Detail = "budget/detail/{budgetName}/{monthlyLimit}/{spent}"
            fun detailRoute(
                budgetName: String,
                monthlyLimit: Double,
                spent: Double,
            ): String = "budget/detail/${budgetName.encodeForRoute()}/$monthlyLimit/$spent"

            private fun String.encodeForRoute(): String = java.net.URLEncoder.encode(this, "UTF-8")
        }

        // ── Analytics tab ─────────────────────────────────────────────────────
        object Analytics {
            const val Root = "analytics"
        }

        // ── Settings tab ──────────────────────────────────────────────────────
        object Settings {
            const val Root = "settings"
            const val PersonalInfo = "settings/personal_info"
            const val LinkedAccounts = "settings/linked_accounts"
            const val Security = "settings/security"
            const val Notifications = "settings/notifications"
            const val Help = "settings/help"
            const val PrivacyPolicy = "settings/privacy_policy"
            const val Terms = "settings/terms"
            const val About = "settings/about"
            const val DeleteAccount = "settings/delete_account"

            const val ChangeContactOtp = "settings/change_contact_otp/{type}"
            fun changeContactOtpRoute(type: String): String = "settings/change_contact_otp/$type"
        }

        // ── Notifications ─────────────────────────────────────────────────────
        object Notifications {
            const val Root = "notifications"
        }

        // ── SMS Pending review ────────────────────────────────────────────────
        object Sms {
            const val PendingReview = "sms/pending_review"
        }

        // ── Report ────────────────────────────────────────────────────────────
        object Report {
            const val Hub = "report/hub"
            const val Create = "report/create"
            const val Detail = "report/detail/{reportId}"
            fun detailRoute(reportId: String) = "report/detail/$reportId"
        }

        // ── Vault ─────────────────────────────────────────────────────────────
        object Vault {
            const val Root = "vault"
        }
    }

    // ── Premium flow ──────────────────────────────────────────────────────────
    object Premium {
        const val Root = "premium"
    }
}