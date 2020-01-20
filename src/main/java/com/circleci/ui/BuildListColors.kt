package com.circleci.ui

import com.intellij.ui.JBColor
import com.intellij.util.ui.UIUtil
import java.awt.Color
import javax.swing.JList

object BuildListColors {

    private val selectionBackground = JBColor(0xE9EEF5, 0x464A4D)
    private val unfocusedSelectionBackground = JBColor(0xF5F5F5, 0x464A4D)

    fun background(list: JList<*>, isSelected: Boolean, hasFocus: Boolean): Color {
        return if (isSelected) {
            if (hasFocus) JBColor.namedColor("Table.lightSelectionBackground", selectionBackground)
            else JBColor.namedColor("Table.lightSelectionInactiveBackground", unfocusedSelectionBackground)
        } else list.background
    }

    fun foreground(isSelected: Boolean, hasFocus: Boolean): Color {
        val default = UIUtil.getListForeground()
        return if (isSelected) {
            if (hasFocus) JBColor.namedColor("Table.lightSelectionForeground", default)
            else JBColor.namedColor("Table.lightSelectionInactiveForeground", default)
        } else JBColor.namedColor("Table.foreground", default)
    }

    fun secondaryForeground(list: JList<*>, isSelected: Boolean): Color {
        return if (isSelected) {
            foreground(true, list.hasFocus())
        } else JBColor.namedColor("Component.infoForeground", UIUtil.getContextHelpForeground())
    }

}