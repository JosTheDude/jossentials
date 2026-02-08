package gg.jos.jossentials.workbenches.command;

import co.aikar.commands.annotation.CommandAlias;
import gg.jos.jossentials.Jossentials;
import gg.jos.jossentials.util.MessageDispatcher;
import gg.jos.jossentials.workbenches.WorkbenchType;

@CommandAlias("%{workbench_stonecutter}")
public final class StonecutterCommand extends WorkbenchCommandBase {
    public StonecutterCommand(Jossentials plugin, MessageDispatcher messageDispatcher) {
        super(plugin, messageDispatcher, WorkbenchType.STONECUTTER);
    }
}
