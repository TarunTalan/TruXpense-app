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
            const val AddExpense = "home/add_expense"
            const val AddExpenseResult = "home/add_expense/result"
            const val AddIncome = "home/add_income"
            const val Savings = "home/savings"
        }

        // ── Transactions tab ──────────────────────────────────────────────────
        object Transactions {
            const val Root = "transactions"
            const val AddExpense = "transactions/add_expense"

            const val Detail = "transactions/detail/{transactionId}"

            fun detailRoute(transactionId: String): String =
                "transactions/detail/${java.net.URLEncoder.encode(transactionId, "UTF-8")}"

            // Edit route for editing an existing expense transaction
            const val Edit = "transactions/edit/{transactionId}"

            fun editRoute(transactionId: String): String =
                "transactions/edit/${java.net.URLEncoder.encode(transactionId, "UTF-8")}"

            // Edit route for editing an existing income entry
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

            /** Percent-encode chars that would break URL segment parsing. */
            private fun String.encodeForRoute(): String = java.net.URLEncoder.encode(this, "UTF-8")
        }

        // ── Analytics tab ─────────────────────────────────────────────────────
        object Analytics {
            const val Root = "analytics"
            const val AddExpense = "analytics/add_expense"
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

            /** type = EMAIL | PHONE */
            const val ChangeContactOtp = "settings/change_contact_otp/{type}"

            fun changeContactOtpRoute(type: String): String =
                "settings/change_contact_otp/$type"
        }

        // ── Notifications (not a bottom-nav tab; shown as a full screen) ──────
        object Notifications {
            const val Root = "notifications"
        }

        // ── SMS Pending review ────────────────────────────────────────────────
        object Sms {
            const val PendingReview = "sms/pending_review"
        }
    }
}