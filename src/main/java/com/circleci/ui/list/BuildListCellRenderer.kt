package com.circleci.ui.list

import com.circleci.api.model.Build
import com.circleci.ui.CircleCIIcons
import com.intellij.icons.AllIcons
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import net.miginfocom.layout.CC
import net.miginfocom.layout.LC
import net.miginfocom.swing.MigLayout
import java.awt.Component
import javax.swing.*

internal class BuildListCellRenderer : ListCellRenderer<Build>, JPanel() {

    private val statusIcon = JLabel()
    private val buildNumber = JLabel()
    private val message = JLabel()
    private val branch = JLabel()

    init {
        border = JBUI.Borders.empty(5, 6)

        layout = MigLayout(LC()
                .gridGap("0", "0")
                .insets("0", "0", "0", "0")
                .fillX())

        val gapAfter = "${JBUI.scale(5)}px"

        statusIcon.verticalAlignment = SwingConstants.CENTER
        add(statusIcon, CC()
                .spanY(3)
                .gapAfter("${JBUI.scale(6)}px"))
        add(buildNumber, CC()
                .minWidth("pref/2px")
                .growX()
                .pushX()
                .gapAfter(gapAfter)
                .wrap())
        add(message, CC()
                .minWidth("pref/2px")
                .wrap()
                .gapAfter(gapAfter))
        add(branch, CC()
                .minWidth("0px")
                .spanX(2))
    }

    override fun getListCellRendererComponent(list: JList<out Build>,
                                              value: Build,
                                              index: Int,
                                              isSelected: Boolean,
                                              cellHasFocus: Boolean): Component {
        UIUtil.setBackgroundRecursively(this, BuildListColors.background(list, isSelected, list.hasFocus()))
        val primaryTextColor = BuildListColors.foreground(isSelected, list.hasFocus())
        val secondaryTextColor = BuildListColors.secondaryForeground(list, isSelected)

        statusIcon.apply {
            icon = when (value.status) {
                "success" -> CircleCIIcons.SUCCESS_BUILD
                "fixed" -> CircleCIIcons.SUCCESS_BUILD
                "failed" -> AllIcons.General.Error
                "running" -> CircleCIIcons.RUNNING_BUILD
                "canceled" -> CircleCIIcons.CANCELED_BUILD
                "not_run" -> CircleCIIcons.CANCELED_BUILD
                "queued" -> CircleCIIcons.ON_HOLD
                else -> CircleCIIcons.RUNNING_BUILD
            }
        }

        buildNumber.apply {
            text = "#${value.buildNumber} ${if (value.workflows != null) value.workflows.jobName else ""}"
            foreground = primaryTextColor
        }

        message.apply {
            text = if (value.subject != null) value.subject else " "
            foreground = secondaryTextColor
        }
        branch.apply {
            text = "${value.branch} by ${value.user.login}"
            foreground = secondaryTextColor
        }
        return this
    }

}