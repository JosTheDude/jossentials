package gg.jos.jossentials.workbenches.command;

import co.aikar.commands.annotation.CommandAlias;
import gg.jos.jossentials.Jossentials;
import gg.jos.jossentials.util.MessageDispatcher;
import gg.jos.jossentials.workbenches.WorkbenchType;

@CommandAlias("%workbench_smithing_table%")
public final class SmithingTableCommand extends WorkbenchCommandBase {
    public SmithingTableCommand(Jossentials plugin, MessageDispatcher messageDispatcher) {
        super(plugin, messageDispatcher, WorkbenchType.SMITHING_TABLE);
    }
}
