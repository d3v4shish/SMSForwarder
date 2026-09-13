package com.example.smsforwarder

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.telephony.PhoneNumberUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val routeStore by lazy { RouteStore(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = color(R.color.background)
        window.navigationBarColor = color(R.color.background)
        render()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_PERMISSIONS_REQUEST) render()
    }

    private fun render() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(28), dp(20), dp(32))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }

        container.addView(heading("SMS Forwarder", 28))

        if (!hasSmsPermissions()) {
            container.addView(
                noticeCard(
                    R.color.warning,
                    "SMS PERMISSION REQUIRED",
                    "Allow SMS access before matching messages can be forwarded.",
                ),
                fullWidthParams(top = 14),
            )
            container.addView(button("Grant SMS permissions", ButtonStyle.PRIMARY) { requestSmsPermissions() }.apply {
                layoutParams = fullWidthParams(top = 10)
            })
        }

        container.addView(heading("Forwarding rules", 20).apply {
            layoutParams = fullWidthParams(top = if (hasSmsPermissions()) 16 else 22)
        })

        val routes = routeStore.load()
        if (routes.isEmpty()) {
            container.addView(emptyRoutesCard(), fullWidthParams(top = 10))
        } else {
            routes.forEachIndexed { index, route ->
                container.addView(routeCard(route, index, routes.size), fullWidthParams(top = 10))
            }
        }
        container.addView(button("Add rule", ButtonStyle.PRIMARY) { showRouteEditor(null) }.apply {
            layoutParams = fullWidthParams(top = 12)
        })

        setContentView(
            ScrollView(this).apply {
                isFillViewport = true
                setBackgroundColor(color(R.color.background))
                addView(container)
            },
        )
    }

    private fun emptyRoutesCard(): View {
        return card().apply {
            setPadding(dp(16), dp(14), dp(16), dp(14))
            addView(heading("No routing rules", 16))
            addView(body("Add a rule to forward matching messages."))
        }
    }

    private fun noticeCard(colorResource: Int, label: String, detail: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = outlinedSurface(color(R.color.surface), color(R.color.border))
            elevation = dp(2).toFloat()
            addView(View(this@MainActivity).apply {
                setBackgroundColor(color(colorResource))
                layoutParams = LinearLayout.LayoutParams(dp(4), ViewGroup.LayoutParams.MATCH_PARENT)
            })
            addView(
                LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                    addView(overline(label, colorResource))
                    addView(body(detail))
                },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
            )
        }
    }

    private fun routeCard(route: Route, index: Int, routeCount: Int): View {
        return card().apply {
            setPadding(dp(16), dp(14), dp(16), dp(12))
            addView(overline("ROUTE ${index + 1}", R.color.primary))
            addView(overline("FORWARD TO"))
            addView(technicalText(route.destination, 17, color(R.color.textPrimary)))
            addView(thinDivider())
            addView(routeCondition(conditionLabel("SENDER", route.matchMode), route.senderRule.ifBlank { "Any sender" }))
            addView(routeCondition(conditionLabel("MESSAGE", route.matchMode), route.messageRule.ifBlank { "Any message" }))
            addView(thinDivider())
            addView(
                LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.START
                    addView(button("Edit", ButtonStyle.SECONDARY) { showRouteEditor(route) })
                    addView(button("Delete", ButtonStyle.DESTRUCTIVE) { confirmDeletion(route) })
                    if (index > 0) addView(button("↑", ButtonStyle.NEUTRAL, "Move route up") { moveRoute(route.id, -1) })
                    if (index < routeCount - 1) addView(button("↓", ButtonStyle.NEUTRAL, "Move route down") { moveRoute(route.id, 1) })
                },
            )
        }
    }

    private fun routeCondition(label: String, value: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4), 0, dp(4))
            addView(overline(label))
            addView(body(value, color(R.color.textPrimary)))
        }
    }

    private fun showRouteEditor(existing: Route?) {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(4), dp(20), 0)
        }
        var matchMode = existing?.matchMode ?: RouteMatchMode.CONTAINS
        val matchStyleButtons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = fullWidthParams(top = 10)
        }
        val containsButton = button("Contains", ButtonStyle.SECONDARY) {}
        val regexButton = button("Regex", ButtonStyle.SECONDARY) {}
        matchStyleButtons.addView(containsButton)
        matchStyleButtons.addView(regexButton)
        form.addView(matchStyleButtons)

        val sender = labeledInput(
            form,
            "SENDER CONTAINS",
            "e.g. BANK",
            existing?.senderRule.orEmpty(),
            technical = true,
        )
        val message = labeledInput(
            form,
            "MESSAGE CONTAINS",
            "e.g. OTP",
            existing?.messageRule.orEmpty(),
            technical = true,
        )
        val destination = labeledInput(
            form,
            "FORWARD TO",
            "+15551234567",
            existing?.destination.orEmpty(),
            technical = true,
        ).also { it.input.inputType = android.text.InputType.TYPE_CLASS_PHONE }
        val modeHelp = caption("")
        form.addView(modeHelp)

        fun updateMatchStyle() {
            val regex = matchMode == RouteMatchMode.REGEX
            styleButton(containsButton, if (regex) ButtonStyle.SECONDARY else ButtonStyle.PRIMARY)
            styleButton(regexButton, if (regex) ButtonStyle.PRIMARY else ButtonStyle.SECONDARY)
            sender.label.text = if (regex) "SENDER REGEX" else "SENDER CONTAINS"
            sender.input.hint = if (regex) "e.g. ^BANK-[A-Z]+$" else "e.g. BANK"
            message.label.text = if (regex) "MESSAGE REGEX" else "MESSAGE CONTAINS"
            message.input.hint = if (regex) "e.g. \\b(?:OTP|PIN)\\b" else "e.g. OTP"
            modeHelp.text = if (regex) {
                "Use ^ and $ for a whole sender; \\bOTP\\b matches a word."
            } else {
                "Matches text anywhere, ignoring case."
            }
        }
        containsButton.setOnClickListener {
            matchMode = RouteMatchMode.CONTAINS
            updateMatchStyle()
        }
        regexButton.setOnClickListener {
            matchMode = RouteMatchMode.REGEX
            updateMatchStyle()
        }
        updateMatchStyle()

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Add routing rule" else "Edit routing rule")
            .setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null)
            .create()
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(outlinedSurface(color(R.color.surface), color(R.color.border)))
            styleDialogButton(dialog.getButton(AlertDialog.BUTTON_NEGATIVE), ButtonStyle.NEUTRAL)
            styleDialogButton(dialog.getButton(AlertDialog.BUTTON_POSITIVE), ButtonStyle.PRIMARY)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val senderCondition = sender.input.text.toString().trim()
                val messageCondition = message.input.text.toString().trim()
                if (senderCondition.isEmpty() && messageCondition.isEmpty()) {
                    Toast.makeText(this, "Enter a sender or message condition.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                if (matchMode == RouteMatchMode.REGEX) {
                    RouteRegex.validationError(senderCondition)?.let { error ->
                        Toast.makeText(this, "Sender regex: $error", Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }
                    RouteRegex.validationError(messageCondition)?.let { error ->
                        Toast.makeText(this, "Message regex: $error", Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }
                }
                val normalizedDestination = normalizeDestination(destination.input.text.toString())
                if (normalizedDestination == null) {
                    Toast.makeText(this, "Enter a valid destination number.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                saveRoute(
                    Route(
                        id = existing?.id ?: nextRouteId(),
                        senderRule = senderCondition,
                        messageRule = messageCondition,
                        destination = normalizedDestination,
                        matchMode = matchMode,
                    ),
                )
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun confirmDeletion(route: Route) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Delete routing rule?")
            .setMessage("Messages matching this rule will no longer be forwarded to ${route.destination}.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete", null)
            .create()
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(outlinedSurface(color(R.color.surface), color(R.color.error)))
            styleDialogButton(dialog.getButton(AlertDialog.BUTTON_NEGATIVE), ButtonStyle.NEUTRAL)
            styleDialogButton(dialog.getButton(AlertDialog.BUTTON_POSITIVE), ButtonStyle.DESTRUCTIVE)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                deleteRoute(route.id)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun saveRoute(route: Route) {
        val routes = routeStore.load().toMutableList()
        val existingIndex = routes.indexOfFirst { it.id == route.id }
        if (existingIndex >= 0) routes[existingIndex] = route else routes.add(route)
        routeStore.save(routes)
        render()
    }

    private fun nextRouteId(): Long = (routeStore.load().maxOfOrNull { it.id } ?: 0L) + 1L

    private fun deleteRoute(routeId: Long) {
        routeStore.save(routeStore.load().filterNot { it.id == routeId })
        render()
    }

    private fun moveRoute(routeId: Long, direction: Int) {
        val routes = routeStore.load().toMutableList()
        val from = routes.indexOfFirst { it.id == routeId }
        val to = from + direction
        if (from >= 0 && to in routes.indices) {
            routes.add(to, routes.removeAt(from))
            routeStore.save(routes)
            render()
        }
    }

    private fun requestSmsPermissions() {
        requestPermissions(
            arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS),
            SMS_PERMISSIONS_REQUEST,
        )
    }

    private fun hasSmsPermissions(): Boolean {
        return checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
    }

    private fun normalizeDestination(value: String): String? {
        return PhoneNumberUtils.normalizeNumber(value).takeIf { it.any(Char::isDigit) }
    }

    private fun labeledInput(
        form: LinearLayout,
        label: String,
        hint: String,
        value: String,
        technical: Boolean = false,
    ): RuleInput {
        val labelView = overline(label).apply { layoutParams = fullWidthParams(top = 10) }
        form.addView(labelView)
        val input = EditText(this).apply {
            this.hint = hint
            setHintTextColor(color(R.color.textSecondary))
            setTextColor(color(R.color.textPrimary))
            setText(value)
            setSingleLine(true)
            textSize = 14f
            typeface = if (technical) Typeface.MONOSPACE else Typeface.DEFAULT
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = outlinedSurface(color(R.color.background), color(R.color.border))
            layoutParams = fullWidthParams(top = 3)
        }
        form.addView(input)
        return RuleInput(labelView, input)
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = outlinedSurface(color(R.color.surface), color(R.color.border))
        elevation = dp(2).toFloat()
    }

    private fun heading(value: String, size: Int): TextView = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(color(R.color.textPrimary))
        typeface = Typeface.DEFAULT_BOLD
        includeFontPadding = false
        setPadding(0, dp(2), 0, dp(4))
    }

    private fun overline(value: String, colorResource: Int = R.color.textSecondary): TextView = TextView(this).apply {
        text = value
        textSize = 12f
        setTextColor(color(colorResource))
        typeface = Typeface.DEFAULT_BOLD
        letterSpacing = 0.08f
        includeFontPadding = false
        setPadding(0, dp(2), 0, dp(3))
    }

    private fun body(value: String, textColor: Int = color(R.color.textSecondary)): TextView = TextView(this).apply {
        text = value
        textSize = 14f
        setTextColor(textColor)
        setLineSpacing(dp(2).toFloat(), 1f)
        setPadding(0, dp(2), 0, dp(3))
    }

    private fun caption(value: String): TextView = TextView(this).apply {
        text = value
        textSize = 12f
        setTextColor(color(R.color.textSecondary))
        setPadding(0, dp(6), 0, dp(2))
    }

    private fun technicalText(value: String, size: Int, textColor: Int): TextView = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(textColor)
        typeface = Typeface.MONOSPACE
        setPadding(0, dp(1), 0, dp(3))
    }

    private fun button(
        label: String,
        style: ButtonStyle,
        accessibilityLabel: String = label,
        onClick: () -> Unit,
    ): Button = Button(this).apply {
        text = label
        contentDescription = accessibilityLabel
        isAllCaps = false
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        minWidth = 0
        minHeight = 0
        minimumWidth = 0
        minimumHeight = 0
        setPadding(dp(12), dp(8), dp(12), dp(8))
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply { marginEnd = dp(8) }
        styleButton(this, style)
        setOnClickListener { onClick() }
    }

    private fun styleDialogButton(button: Button, style: ButtonStyle) {
        button.isAllCaps = false
        button.textSize = 14f
        button.typeface = Typeface.DEFAULT_BOLD
        button.setPadding(dp(12), dp(6), dp(12), dp(6))
        styleButton(button, style)
    }

    private fun styleButton(button: Button, style: ButtonStyle) {
        val fill: Int
        val border: Int
        val text: Int
        val elevation: Int
        when (style) {
            ButtonStyle.PRIMARY -> {
                fill = R.color.primary; border = R.color.primary; text = R.color.surface; elevation = 2
            }
            ButtonStyle.SECONDARY -> {
                fill = R.color.surface; border = R.color.primary; text = R.color.primary; elevation = 1
            }
            ButtonStyle.DESTRUCTIVE -> {
                fill = R.color.surface; border = R.color.error; text = R.color.error; elevation = 1
            }
            ButtonStyle.NEUTRAL -> {
                fill = R.color.surface; border = R.color.border; text = R.color.textPrimary; elevation = 0
            }
        }
        button.background = outlinedSurface(color(fill), color(border))
        button.setTextColor(color(text))
        button.elevation = dp(elevation).toFloat()
    }

    private fun outlinedSurface(fill: Int, border: Int, borderWidth: Int = 1): GradientDrawable = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dp(3).toFloat()
        setStroke(dp(borderWidth), border)
    }

    private fun thinDivider(): View = View(this).apply {
        setBackgroundColor(color(R.color.border))
        layoutParams = fullWidthParams(top = 8, bottom = 8, height = 1)
    }

    private fun fullWidthParams(
        top: Int = 0,
        bottom: Int = 0,
        height: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
    ): LinearLayout.LayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height).apply {
        topMargin = dp(top)
        bottomMargin = dp(bottom)
    }

    private fun color(resource: Int): Int = getColor(resource)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private enum class ButtonStyle { PRIMARY, SECONDARY, DESTRUCTIVE, NEUTRAL }

    private data class RuleInput(val label: TextView, val input: EditText)

    private fun conditionLabel(subject: String, mode: RouteMatchMode): String {
        return if (mode == RouteMatchMode.CONTAINS) "$subject CONTAINS" else "$subject REGEX"
    }

    private companion object {
        const val SMS_PERMISSIONS_REQUEST = 100
    }
}
