package gg.jos.jossentials.workbenches.command;

import co.aikar.commands.annotation.CommandAlias;
import gg.jos.jossentials.Jossentials;
import gg.jos.jossentials.util.MessageDispatcher;
import gg.jos.jossentials.workbenches.WorkbenchType;

@CommandAlias("%{workbench_furnace}")
public final class FurnaceCommand extends WorkbenchCommandBase {
    public FurnaceCommand(Jossentials plugin, MessageDispatcher messageDispatcher) {
        super(plugin, messageDispatcher, WorkbenchType.FURNACE);
    }
}
