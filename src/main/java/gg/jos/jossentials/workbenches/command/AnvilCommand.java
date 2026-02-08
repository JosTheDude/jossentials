package gg.jos.jossentials.workbenches.command;

import co.aikar.commands.annotation.CommandAlias;
import gg.jos.jossentials.Jossentials;
import gg.jos.jossentials.util.MessageDispatcher;
import gg.jos.jossentials.workbenches.WorkbenchType;

@CommandAlias("%{workbench_anvil}")
public final class AnvilCommand extends WorkbenchCommandBase {
    public AnvilCommand(Jossentials plugin, MessageDispatcher messageDispatcher) {
        super(plugin, messageDispatcher, WorkbenchType.ANVIL);
    }
}
