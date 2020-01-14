package com.nguyennk.action

import com.intellij.icons.AllIcons

class DebugTsAction : TsAction(icon = AllIcons.Actions.StartDebugger) {
    override val debug = true
}
