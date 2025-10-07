package org.example;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
    public final static String inputFilePath = "C:\\path\\to\\inputfile.csv";
    public final static String outputFilePath = "C:\\path\\to\\outputfile.csv";

    public static void main(String[] args) throws Exception {
        CSVReader csvReader = new CSVReader(new FileReader(inputFilePath));
        List<String[]> list = new ArrayList<>();
        String[] line;
        while ((line = csvReader.readNext()) != null) {
            list.add(line);
        }
        List<ReadEntry> readList = new ArrayList<>();
        list.remove(0);
        List<List<String>> newList = new ArrayList<List<String>>();
        for (String[] entry : list) {
            newList.add(Arrays.asList(entry));
        }
        List<List<String>> realList = new ArrayList<List<String>>();
        for (int i = 0; i < newList.size(); i++) {
            for (List<String> content : newList) {
                if ((content.get(content.size() - 1)).equals("")) {
                    //skip entries with an empty eventID
                }
                //Sort by EventId
                else if (Integer.parseInt(content.get(content.size() - 1)) == i + 1) {
                         realList.add(content);
                }
            }
        }
        int realListIndex = 0;
        for (List<String> entry : realList) {

            if (entry.get(6).equals("DRAGOUTSIDE") || entry.get(6).equals("ENDDRAGONTO")) {
                //Remove DragOutside events
            } else if (entry.get(6).equals("MOVE") && entry.get(16).equals("")) {
                //Remove Move events that have no BlockId (Ghost events)
            } else if (entry.get(6).equals("ENDDRAG") && entry.get(16).equals("")) {
                //Remove Enddrag events that have no BlockId (Ghost events)
            } else if (entry.get(5).equals("BUTTON")) {
                //Remove events caused by clicking on buttons of the experimental debugger
            } else if (entry.get(6).equals("COMMENT_CREATE") || entry.get(6).equals("COMMENT_CHANGE") || entry.get(6).equals("COMMENT_DELETE")) {
                //Remove comment create, change and delete events for better readability (creating comments has no bearing on the program)
            } else if (entry.get(5).equals("ADD") || entry.get(5).equals("RENAME")) {
                //Remove events that add resources or rename variables, sprites or resources
            } else if (entry.get(6).equals("MOVE") && entry.get(17).equals("") && realList.get(realListIndex - 1).get(5).equals("CONTEXTMENU")) {
                //Remove Move events that are caused immediately after a Contextmenu/Dropdownmenu Event (these are not real events)
            } else {
                ReadEntry newEntry = new ReadEntry();
                newEntry.setId(entry.get(0));
                newEntry.setUser(entry.get(1));
                newEntry.setUsername(entry.get(2));
                newEntry.setExperiment(entry.get(3));
                newEntry.setDate(entry.get(4));
                newEntry.setEventType(entry.get(5));
                newEntry.setEvent(entry.get(6));
                newEntry.setSpritename(entry.get(7));
                newEntry.setMetadata(entry.get(8));
                newEntry.setBlockId(entry.get(16));
                newEntry.setGroup(entry.get(17));
                newEntry.setScriptId(entry.get(18));
                newEntry.setScriptIdAfterMove(entry.get(19));
                newEntry.setOldParentId(entry.get(20));
                newEntry.setNewParentId(entry.get(21));
                newEntry.setValueType(entry.get(22));
                newEntry.setOldValue(entry.get(23));
                newEntry.setNewValue(entry.get(24));
                newEntry.setCategory(entry.get(25));
                newEntry.setOldInputName(entry.get(26));
                newEntry.setNewInputName(entry.get(27));
                newEntry.setLoopInvolved(Boolean.parseBoolean(entry.get(28)));
                newEntry.setConditionalInvolved(Boolean.parseBoolean(entry.get(29)));
                newEntry.setBlockCount(entry.get(30));
                newEntry.setEventId(entry.get(31));
                readList.add(newEntry);
            }
            realListIndex++;
        }


        List<List<ReadEntry>> newReadList = combineSameGroupEntries(readList);

        List<WriteEntry> writeList = new ArrayList<WriteEntry>();
        for (List<ReadEntry> entry : newReadList) {
            writeList.add(handleGroup(newReadList, entry));
        }

        List<String[]> writeListCSV = new ArrayList<String[]>();
        String[] headerArray = new String[]{"ActionType", "Begin", "End", "Metadata", "Spritename", "BlockID", "BlockCount",
        "Category", "ScriptID", "ScriptIDAfterMove", "OldParentID", "NewParentID", "ValueType", "OldValue", "NewValue", "OldInputName", "NewInputName",
        "ConditionalInvolved", "LoopInvolved", "Group"};
        writeListCSV.add(headerArray);
        for (WriteEntry writeEntry : writeList) {
            String[] array = new String[20];
            array[0] = writeEntry.getActionType().toString();
            array[1] = writeEntry.getBegin();
            array[2] = writeEntry.getEnd();
            array[3] = writeEntry.metadata;
            array[4] = writeEntry.getSpritename();
            array[5] = writeEntry.blockID;
            array[6] = writeEntry.blockCount;
            array[7] = writeEntry.category;
            array[8] = writeEntry.scriptId;
            array[9] = writeEntry.scriptIdAfterMove;
            array[10] = writeEntry.oldParentId;
            array[11] = writeEntry.newParentId;
            array[12] = writeEntry.valueType;
            array[13] = writeEntry.oldValue;
            array[14] = writeEntry.newValue;
            array[15] = writeEntry.oldInputName;
            array[16] = writeEntry.newInputName;
            if (writeEntry.conditionalInvolved != null) array[17] = writeEntry.conditionalInvolved;
            if (writeEntry.loopInvolved != null) array[18] = writeEntry.loopInvolved;
            array[19] = writeEntry.getGroup();
            writeListCSV.add(array);
        }
        writeDataAtOnce(outputFilePath, writeListCSV);

    }

    public static WriteEntry handleGroup(List<List<ReadEntry>> readList, List<ReadEntry> groupEntries) {

        ReadEntry firstEntry = groupEntries.get(0);
        if (groupEntries.size() == 1) {
            return handleSingleEntry(firstEntry);
        }

        //Remove move events from blocks other than the main block which do not change the program order in any way
        //This can happen for example when moving a loop block directly in front of an operator block which do not fit
        //together
        removeMeaninglessMoveEventsAtTheEnd(groupEntries);

        removeInactivityEventsInList(groupEntries);

        ReadEntry tmpLastEntry = groupEntries.get(groupEntries.size() - 1);

        // If a movement replaces an actual input block with another actual input block (like when moving an operator
        // block into another block that already contains an operator block), the original input block gets moved into
        // the workspace with its newParentId being empty. This move of the original input block is then the last event
        // in the group which would confuse the parser which is why it is deleted
        if (!firstEntry.blockId.equals("") && !tmpLastEntry.blockId.equals("") && tmpLastEntry.event.equals("MOVE") && !firstEntry.blockId.equals(tmpLastEntry.blockId) && tmpLastEntry.newParentId.equals("")){
            groupEntries.remove(groupEntries.size() - 1);
        }

        ReadEntry secondEntry = groupEntries.get(1); //Important for groups that begin with a CREATE or ENDDRAG event (These events lack some attributes)
        ReadEntry secondToLastEntry = groupEntries.get(groupEntries.size() - 2); //Useful for Contextmenu Events
        List<String> otherInvolvedBlocks = new ArrayList<String>();
        for (ReadEntry readEntry: groupEntries) {
            if (!otherInvolvedBlocks.contains(readEntry.blockId)) {
                otherInvolvedBlocks.add(readEntry.blockId);
            }
        }
        ReadEntry lastEntry = groupEntries.get(groupEntries.size() - 1);
        ReadEntry firstMoveEntry = getfirstEntryWithEvent(groupEntries, "MOVE");


        //Special case for when a C-shaped block without inputs and without parent gets moved to another block(stack)
        //and this block(stack) becomes a new input for the C-shaped block. This needs to be handled differently because
        //usually blocks can only become inputs when they themselves were actively moved and this affects the parsing
        if (firstMoveEntry != null && (firstMoveEntry.category.equals("conditional") || firstMoveEntry.category.equals("loop")) && lastEntry.newParentId.equals(firstMoveEntry.blockId) && !lastEntry.newInputName.equals("")) {
            WriteEntry newEntry = new WriteEntry();
            newEntry.setSpritename(firstMoveEntry.spritename);
            newEntry.setGroup(firstMoveEntry.group);
            newEntry.setBlockID(firstMoveEntry.blockId);
            newEntry.setCategory(firstMoveEntry.category);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            newEntry.setScriptIdAfterMove(secondToLastEntry.scriptIdAfterMove);
            newEntry.setNewInputName(secondToLastEntry.newInputName);
            newEntry.setNewParentId(secondToLastEntry.newParentId);
            if (lastEntry.conditionalInvolved) newEntry.setConditionalInvolved("true");
            if (lastEntry.loopInvolved) newEntry.setLoopInvolved("true");
            if (groupEntries.size() == 1) {
                newEntry.setMetadata(groupEntries.size() + " event");
            } else {
                newEntry.setMetadata(groupEntries.size() + " events");
            }
            if (firstEntry.event.equals("CREATE")) {
                if (secondToLastEntry.scriptIdAfterMove.equals("")) {
                    newEntry.setActionType(ActionTypes.ADD_NEW_BLOCK_TO_NONPROGRAM);
                } else {
                    newEntry.setActionType(ActionTypes.ADD_NEW_BLOCK_TO_PROGRAM);
                }
            } else if (firstEntry.event.equals("ENDDRAG")) {
                newEntry.setBlockCount(firstMoveEntry.blockCount);
                if (secondToLastEntry.scriptIdAfterMove.equals("")) {
                    newEntry.setActionType(ActionTypes.MOVE_FROM_NONPROGRAM_TO_NONPROGAM);
                } else {
                    newEntry.setActionType(ActionTypes.MOVE_FROM_NONPROGRAM_TO_PROGAM);
                }
            } else if (firstEntry.event.equals("MOVE")) {
                newEntry.setBlockCount(firstEntry.blockCount);
                newEntry.setOldParentId(firstEntry.oldParentId);
                newEntry.setOldInputName(firstEntry.oldInputName);
                newEntry.setScriptId(firstEntry.scriptId);
                if (secondToLastEntry.scriptIdAfterMove.equals("")) {
                    newEntry.setActionType(ActionTypes.MOVE_FROM_PROGRAM_TO_NONPROGRAM);
                } else {
                    newEntry.setActionType(ActionTypes.MOVE_FROM_PROGRAM_TO_PROGAM);
                }
            }
            return newEntry;
        }
        else if (firstEntry.event.equals("CREATE") && lastEntry.event.equals("MOVE") && firstEntry.blockId.equals(lastEntry.blockId) && !lastEntry.scriptId.equals("") && lastEntry.scriptId.equals(lastEntry.blockId)) {
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.ADD_HAT_TO_WORKSPACE);
            newEntry.setGroup(firstEntry.group);
            newEntry.setCategory(firstEntry.category);
            newEntry.setBlockID(firstEntry.blockId);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            newEntry.setScriptId(firstEntry.scriptId);
            newEntry.setScriptIdAfterMove(firstEntry.scriptId);
            newEntry.setSpritename(firstEntry.spritename);
            if (groupEntries.size() == 1) {
                newEntry.setMetadata(groupEntries.size() + " event");
            } else {
                newEntry.setMetadata(groupEntries.size() + " events");
            }
            return newEntry;
        }
        else if (firstEntry.event.equals("CREATE") && lastEntry.event.equals("MOVE") && !firstEntry.blockId.equals(lastEntry.blockId) && lastEntry.scriptIdAfterMove.equals(firstEntry.blockId)) {
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.ADD_HAT_TO_NONPROGRAM);
            newEntry.setGroup(firstEntry.group);
            newEntry.setCategory(firstEntry.category);
            newEntry.setBlockID(firstEntry.blockId);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            newEntry.setScriptId(firstEntry.scriptId);
            newEntry.setScriptIdAfterMove(firstEntry.scriptId);
            newEntry.setSpritename(firstEntry.spritename);
            if (groupEntries.size() == 1) {
                newEntry.setMetadata(groupEntries.size() + " event");
            } else {
                newEntry.setMetadata(groupEntries.size() + " events");
            }
            return newEntry;
        }
        else if (firstEntry.event.equals("CREATE") && lastEntry.event.equals("MOVE") && firstEntry.blockId.equals(lastEntry.blockId) && !lastEntry.scriptIdAfterMove.equals("") && !lastEntry.newParentId.equals("")) {
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.ADD_NEW_BLOCK_TO_PROGRAM);
            newEntry.setGroup(firstEntry.group);
            newEntry.setSpritename(firstEntry.spritename);
            newEntry.setBlockID(firstEntry.blockId);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            newEntry.setCategory(lastEntry.category);
            newEntry.setScriptIdAfterMove(lastEntry.scriptIdAfterMove);
            if (lastEntry.conditionalInvolved) newEntry.setConditionalInvolved("true");
            if (lastEntry.loopInvolved) newEntry.setLoopInvolved("true");
            newEntry.setNewParentId(lastEntry.newParentId);
            newEntry.setNewInputName(lastEntry.newInputName);
            if (groupEntries.size() == 1) {
                newEntry.setMetadata(groupEntries.size() + " event");
            } else {
                newEntry.setMetadata(groupEntries.size() + " events");
            }
            return newEntry;
        }
        else if (firstEntry.event.equals("CREATE") && lastEntry.event.equals("MOVE") && firstEntry.blockId.equals(lastEntry.blockId) && lastEntry.scriptIdAfterMove.equals("") && !lastEntry.newParentId.equals("")){
            WriteEntry newEntry = new WriteEntry();
            //Add block from below/insert
            newEntry.setActionType(ActionTypes.ADD_NEW_BLOCK_TO_NONPROGRAM);
            newEntry.setGroup(firstEntry.group);
            newEntry.setSpritename(firstEntry.spritename);
            newEntry.setBlockID(firstEntry.blockId);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            newEntry.setCategory(secondEntry.category);
            if (lastEntry.conditionalInvolved) newEntry.setConditionalInvolved("true");
            if (lastEntry.loopInvolved) newEntry.setLoopInvolved("true");
            newEntry.setNewParentId(lastEntry.newParentId);
            newEntry.setNewInputName(lastEntry.newInputName);
            if (groupEntries.size() == 1) {
                newEntry.setMetadata(groupEntries.size() + " event");
            } else {
                newEntry.setMetadata(groupEntries.size() + " events");
            }
            return newEntry;
        }
        else if (firstEntry.event.equals("CREATE") && lastEntry.event.equals("MOVE") && lastEntry.scriptIdAfterMove.equals("") && firstEntry.blockId.equals(lastEntry.newParentId)){
            WriteEntry newEntry = new WriteEntry();
            //Add block from above
            newEntry.setActionType(ActionTypes.ADD_NEW_BLOCK_TO_NONPROGRAM);
            newEntry.setGroup(firstEntry.group);
            newEntry.setSpritename(firstEntry.spritename);
            newEntry.setBlockID(firstEntry.blockId);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            newEntry.setCategory(secondEntry.category);
            if (groupEntries.size() == 1) {
                newEntry.setMetadata(groupEntries.size() + " event");
            } else {
                newEntry.setMetadata(groupEntries.size() + " events");
            }
            return newEntry;
        }
        else if (firstEntry.event.equals("CREATE") && lastEntry.event.equals("MOVE") && lastEntry.blockId.equals(firstEntry.blockId) && lastEntry.scriptIdAfterMove.equals("")  && lastEntry.newParentId.equals("")) {
            WriteEntry newEntry = new WriteEntry();
            newEntry.setGroup(firstEntry.group);
            newEntry.setActionType(ActionTypes.ADD_NEW_BLOCK_TO_WORKSPACE);
            newEntry.setSpritename(firstEntry.spritename);
            newEntry.setBlockID(firstEntry.blockId);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            newEntry.setCategory(lastEntry.category);
            if (groupEntries.size() == 1) {
                newEntry.setMetadata(groupEntries.size() + " event");
            } else {
                newEntry.setMetadata(groupEntries.size() + " events");
            }
            return newEntry;
        }
        else if (groupEntries.size() == 2 && firstEntry.event.equals("MOVE") && lastEntry.event.equals("DELETE") && !lastEntry.blockId.equals(firstEntry.blockId)) {
            //Special case where using the backspace key can delete the top block of a stack, causing its next block
            //to be detached and the block to be deleted
            WriteEntry newEntry = new WriteEntry();
            newEntry.setGroup(lastEntry.group);
            if (firstEntry.scriptId.equals(lastEntry.blockId)) {
                newEntry.setActionType(ActionTypes.REMOVE_FROM_PROGRAM_AND_DELETE);
            } else {
                newEntry.setActionType(ActionTypes.REMOVE_FROM_NONPROGRAM_AND_DELETE);
            }
            newEntry.setSpritename(lastEntry.spritename);
            newEntry.setBlockID(lastEntry.blockId);
            newEntry.setBlockCount(lastEntry.blockCount);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            newEntry.setCategory(lastEntry.category);
            if (groupEntries.size() == 1) {
                newEntry.setMetadata(groupEntries.size() + " event");
            } else {
                newEntry.setMetadata(groupEntries.size() + " events");
            }
            return newEntry;
        }
        else if (firstEntry.event.equals("MOVE") && lastEntry.event.equals("MOVE") && firstEntry.blockId.equals(lastEntry.blockId) && firstEntry.scriptId.equals(lastEntry.scriptIdAfterMove) && firstEntry.oldParentId.equals(lastEntry.newParentId) && firstEntry.oldInputName.equals(lastEntry.newInputName)) {
            //A move that detaches a block and then attaches it back to where it was detached from
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.MOVE_WITHOUT_CHANGES);
            newEntry.setGroup(firstEntry.group);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            newEntry.setSpritename(firstEntry.spritename);
            if (groupEntries.size() == 1) {
                newEntry.setMetadata(groupEntries.size() + " event");
            } else {
                newEntry.setMetadata(groupEntries.size() + " events");
            }
            return newEntry;
        }
        else if (firstEntry.event.equals("MOVE") && lastEntry.event.equals("MOVE") && firstEntry.blockId.equals(lastEntry.blockId) && !firstEntry.scriptId.equals("") && !lastEntry.scriptIdAfterMove.equals("") && !firstEntry.oldParentId.equals("") && !lastEntry.newParentId.equals("")) {
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.MOVE_FROM_PROGRAM_TO_PROGAM);
            newEntry.setGroup(firstEntry.group);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            newEntry.setCategory(firstEntry.category);
            newEntry.setBlockCount(firstEntry.blockCount);
            newEntry.setSpritename(firstEntry.spritename);
            newEntry.setOldParentId(firstEntry.oldParentId);
            newEntry.setNewParentId(lastEntry.newParentId);
            newEntry.setOldInputName(firstEntry.oldInputName);
            newEntry.setNewInputName(lastEntry.newInputName);
            newEntry.setBlockID(firstEntry.blockId);
            newEntry.setScriptId(firstEntry.scriptId);
            newEntry.setScriptIdAfterMove(lastEntry.scriptIdAfterMove);
            if (firstEntry.conditionalInvolved || lastEntry.conditionalInvolved) newEntry.setConditionalInvolved("true");
            if (firstEntry.loopInvolved || lastEntry.loopInvolved) newEntry.setLoopInvolved("true");
            if (groupEntries.size() == 1) {
                newEntry.setMetadata(groupEntries.size() + " event");
            } else {
                newEntry.setMetadata(groupEntries.size() + " events");
            }
            return newEntry;
        }
        else if (firstEntry.event.equals("MOVE") && lastEntry.event.equals("MOVE") && firstEntry.blockId.equals(lastEntry.blockId) && !firstEntry.scriptId.equals("") && lastEntry.scriptIdAfterMove.equals("") && !lastEntry.newParentId.equals("")) {
            WriteEntry newEntry = new WriteEntry();
            //Move below or insert (attach to another block)
            newEntry.setActionType(ActionTypes.MOVE_FROM_PROGRAM_TO_NONPROGRAM);
            newEntry.setGroup(firstEntry.group);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            newEntry.setCategory(firstEntry.category);
            newEntry.setBlockCount(firstEntry.blockCount);
            newEntry.setSpritename(firstEntry.spritename);
            newEntry.setOldParentId(firstEntry.oldParentId);
            newEntry.setNewParentId(lastEntry.newParentId);
            newEntry.setOldInputName(firstEntry.oldInputName);
            newEntry.setNewInputName(lastEntry.newInputName);
            newEntry.setBlockID(firstEntry.blockId);
            newEntry.setScriptId(firstMoveEntry.scriptId);
            if (firstEntry.conditionalInvolved || lastEntry.conditionalInvolved) newEntry.setConditionalInvolved("true");
            if (firstEntry.loopInvolved || lastEntry.loopInvolved) newEntry.setLoopInvolved("true");
            if (groupEntries.size() == 1) {
                newEntry.setMetadata(groupEntries.size() + " event");
            } else {
                newEntry.setMetadata(groupEntries.size() + " events");
            }
            return newEntry;
        }
        else if (firstEntry.event.equals("MOVE") && lastEntry.event.equals("MOVE") && !firstEntry.blockId.equals(lastEntry.blockId) && !firstEntry.scriptId.equals("") && lastEntry.scriptIdAfterMove.equals("") && !lastEntry.newParentId.equals("")) {
            WriteEntry newEntry = new WriteEntry();
            //Move on top of another block
            newEntry.setActionType(ActionTypes.MOVE_FROM_PROGRAM_TO_NONPROGRAM);
            newEntry.setGroup(firstEntry.group);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            newEntry.setCategory(firstEntry.category);
            newEntry.setBlockCount(firstEntry.blockCount);
            newEntry.setSpritename(firstEntry.spritename);
            newEntry.setOldParentId(firstEntry.oldParentId);
            newEntry.setOldInputName(firstEntry.oldInputName);
            newEntry.setBlockID(firstEntry.blockId);
            newEntry.setScriptId(firstEntry.scriptId);
            if (firstEntry.conditionalInvolved || lastEntry.conditionalInvolved) newEntry.setConditionalInvolved("true");
            if (firstEntry.loopInvolved || lastEntry.loopInvolved) newEntry.setLoopInvolved("true");
            if (groupEntries.size() == 1) {
                newEntry.setMetadata(groupEntries.size() + " event");
            } else {
                newEntry.setMetadata(groupEntries.size() + " events");
            }
            return newEntry;
        }
        else if (firstEntry.event.equals("MOVE") && lastEntry.event.equals("MOVE") && firstEntry.blockId.equals(lastEntry.blockId) && !firstEntry.scriptId.equals("") && lastEntry.scriptIdAfterMove.equals("") && lastEntry.newParentId.equals("")) {
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.MOVE_FROM_PROGRAM_TO_WORKSPACE);
            newEntry.setGroup(firstEntry.group);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            newEntry.setCategory(firstEntry.category);
            newEntry.setBlockCount(firstEntry.blockCount);
            newEntry.setSpritename(firstEntry.spritename);
            newEntry.setOldParentId(firstEntry.oldParentId);
            newEntry.setOldInputName(firstEntry.oldInputName);
            newEntry.setBlockID(firstEntry.blockId);
            newEntry.setScriptId(firstEntry.scriptId);
            if (firstEntry.conditionalInvolved) newEntry.setConditionalInvolved("true");
            if (firstEntry.loopInvolved) newEntry.setLoopInvolved("true");
            if (groupEntries.size() == 1) {
                newEntry.setMetadata(groupEntries.size() + " event");
            } else {
                newEntry.setMetadata(groupEntries.size() + " events");
            }
            return newEntry;
        }
        else if (firstEntry.event.equals("MOVE") && lastEntry.event.equals("MOVE") && firstEntry.blockId.equals(lastEntry.blockId) && firstEntry.scriptId.equals("") && !lastEntry.scriptIdAfterMove.equals("")) {
            WriteEntry newEntry = new WriteEntry();
            //Detach from nonprogram
            newEntry.setActionType(ActionTypes.MOVE_FROM_NONPROGRAM_TO_PROGAM);
            newEntry.setGroup(firstEntry.group);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            newEntry.setCategory(firstEntry.category);
            newEntry.setBlockCount(firstEntry.blockCount);
            newEntry.setSpritename(firstEntry.spritename);
            newEntry.setOldParentId(firstEntry.oldParentId);
            newEntry.setOldInputName(firstEntry.oldInputName);
            newEntry.setBlockID(firstEntry.blockId);
            newEntry.setNewParentId(lastEntry.newParentId);
            newEntry.setNewInputName(lastEntry.newInputName);
            newEntry.setScriptIdAfterMove(lastEntry.scriptIdAfterMove);
            if (firstEntry.conditionalInvolved || lastEntry.conditionalInvolved) newEntry.setConditionalInvolved("true");
            if (firstEntry.loopInvolved || lastEntry.loopInvolved) newEntry.setLoopInvolved("true");
            if (groupEntries.size() == 1) {
                newEntry.setMetadata(groupEntries.size() + " event");
            } else {
                newEntry.setMetadata(groupEntries.size() + " events");
            }
            return newEntry;
        }
        else if (firstEntry.event.equals("ENDDRAG") && lastEntry.event.equals("MOVE") && firstEntry.blockId.equals(lastEntry.blockId) && lastEntry.scriptId.equals("") && !lastEntry.scriptIdAfterMove.equals("")) {
            WriteEntry newEntry = new WriteEntry();
            //No detaching from nonprogram happened
            newEntry.setActionType(ActionTypes.MOVE_FROM_NONPROGRAM_TO_PROGAM);
            newEntry.setGroup(firstEntry.group);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            newEntry.setCategory(firstEntry.category);
            newEntry.setBlockCount(lastEntry.blockCount);
            newEntry.setSpritename(firstEntry.spritename);
            newEntry.setOldParentId(lastEntry.oldParentId);
            newEntry.setOldInputName(lastEntry.oldInputName);
            newEntry.setNewParentId(lastEntry.newParentId);
            newEntry.setNewInputName(lastEntry.newInputName);
            newEntry.setScriptIdAfterMove(lastEntry.scriptIdAfterMove);
            newEntry.setBlockID(firstEntry.blockId);
            if (lastEntry.conditionalInvolved) newEntry.setConditionalInvolved("true");
            if (lastEntry.loopInvolved) newEntry.setLoopInvolved("true");
            if (groupEntries.size() == 1) {
                newEntry.setMetadata(groupEntries.size() + " event");
            } else {
                newEntry.setMetadata(groupEntries.size() + " events");
            }
            return newEntry;
        }
        else if (firstEntry.event.equals("ENDDRAG") && lastEntry.event.equals("MOVE") && !firstEntry.blockId.equals(lastEntry.blockId) && lastEntry.scriptId.equals("") && lastEntry.scriptIdAfterMove.equals(firstEntry.blockId)) {
            WriteEntry newEntry = new WriteEntry();
            //No detaching from nonprogram happened
            //Moving a script on top of nonprogram block(s) is also counted as this (there is no effective difference)
            newEntry.setActionType(ActionTypes.MOVE_FROM_NONPROGRAM_TO_PROGAM);
            newEntry.setGroup(firstEntry.group);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            newEntry.setCategory(lastEntry.category);
            newEntry.setBlockCount(lastEntry.blockCount);
            newEntry.setSpritename(firstEntry.spritename);
            newEntry.setOldParentId(lastEntry.oldParentId);
            newEntry.setOldInputName(lastEntry.oldInputName);
            newEntry.setNewParentId(lastEntry.newParentId);
            newEntry.setNewInputName(lastEntry.newInputName);
            newEntry.setBlockID(lastEntry.blockId);
            newEntry.setScriptIdAfterMove(lastEntry.scriptIdAfterMove);
            if (lastEntry.conditionalInvolved) newEntry.setConditionalInvolved("true");
            if (lastEntry.loopInvolved) newEntry.setLoopInvolved("true");
            if (groupEntries.size() == 1) {
                newEntry.setMetadata(groupEntries.size() + " event");
            } else {
                newEntry.setMetadata(groupEntries.size() + " events");
            }
            return newEntry;
        }
        else if (firstEntry.event.equals("MOVE") && lastEntry.event.equals("MOVE") && firstEntry.blockId.equals(lastEntry.blockId) && firstEntry.scriptId.equals("") && lastEntry.scriptIdAfterMove.equals("") && !lastEntry.newParentId.equals("")) {
            WriteEntry newEntry = new WriteEntry();
            //Detach from nonprogram and move below or insert
            newEntry.setActionType(ActionTypes.MOVE_FROM_NONPROGRAM_TO_NONPROGAM);
            newEntry.setGroup(firstEntry.group);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            newEntry.setCategory(firstEntry.category);
            newEntry.setBlockCount(firstEntry.blockCount);
            newEntry.setSpritename(firstEntry.spritename);
            newEntry.setOldParentId(firstEntry.oldParentId);
            newEntry.setOldInputName(firstEntry.oldInputName);
            newEntry.setNewParentId(lastEntry.newParentId);
            newEntry.setNewInputName(lastEntry.newInputName);
            newEntry.setBlockID(firstEntry.blockId);
            if (firstEntry.conditionalInvolved || lastEntry.conditionalInvolved) newEntry.setConditionalInvolved("true");
            if (firstEntry.loopInvolved || lastEntry.loopInvolved) newEntry.setLoopInvolved("true");
            if (groupEntries.size() == 1) {
                newEntry.setMetadata(groupEntries.size() + " event");
            } else {
                newEntry.setMetadata(groupEntries.size() + " events");
            }
            return newEntry;
        }
        else if (firstEntry.event.equals("MOVE") && lastEntry.event.equals("MOVE") && !firstEntry.blockId.equals(lastEntry.blockId) && firstEntry.scriptId.equals("") && lastEntry.scriptIdAfterMove.equals("") && !lastEntry.newParentId.equals("")) {
            WriteEntry newEntry = new WriteEntry();
            //Detach from nonprogram and move on top
            newEntry.setActionType(ActionTypes.MOVE_FROM_NONPROGRAM_TO_NONPROGAM);
            newEntry.setGroup(firstEntry.group);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            newEntry.setCategory(firstEntry.category);
            newEntry.setBlockCount(firstEntry.blockCount);
            newEntry.setSpritename(firstEntry.spritename);
            newEntry.setOldParentId(firstEntry.oldParentId);
            newEntry.setOldInputName(firstEntry.oldInputName);
            newEntry.setBlockID(firstEntry.blockId);
            if (firstEntry.conditionalInvolved || lastEntry.conditionalInvolved) newEntry.setConditionalInvolved("true");
            if (firstEntry.loopInvolved || lastEntry.loopInvolved) newEntry.setLoopInvolved("true");
            if (groupEntries.size() == 1) {
                newEntry.setMetadata(groupEntries.size() + " event");
            } else {
                newEntry.setMetadata(groupEntries.size() + " events");
            }
            return newEntry;
        }
        else if (firstEntry.event.equals("ENDDRAG") && lastEntry.event.equals("MOVE") && firstEntry.blockId.equals(lastEntry.blockId) && lastEntry.scriptId.equals("")  && !lastEntry.newParentId.equals("")) {
            WriteEntry newEntry = new WriteEntry();
            //No detaching from nonprogram and move below or insert
            newEntry.setActionType(ActionTypes.MOVE_FROM_NONPROGRAM_TO_NONPROGAM);
            newEntry.setGroup(firstEntry.group);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            newEntry.setCategory(firstEntry.category);
            newEntry.setBlockCount(firstEntry.blockCount);
            newEntry.setSpritename(firstEntry.spritename);
            newEntry.setNewParentId(lastEntry.newParentId);
            newEntry.setNewInputName(lastEntry.newInputName);
            newEntry.setBlockID(firstEntry.blockId);
            if (lastEntry.conditionalInvolved) newEntry.setConditionalInvolved("true");
            if (lastEntry.loopInvolved) newEntry.setLoopInvolved("true");
            if (groupEntries.size() == 1) {
                newEntry.setMetadata(groupEntries.size() + " event");
            } else {
                newEntry.setMetadata(groupEntries.size() + " events");
            }
            return newEntry;
        }
        else if (firstEntry.event.equals("ENDDRAG") && lastEntry.event.equals("MOVE") && !firstEntry.blockId.equals(lastEntry.blockId) && lastEntry.scriptId.equals("") && lastEntry.scriptIdAfterMove.equals("") && !lastEntry.newParentId.equals("")) {
            WriteEntry newEntry = new WriteEntry();
            //No detaching from nonprogram and move on top
            //This is handled as if the lower block(stack) was attached to the upper block(stack)
            newEntry.setActionType(ActionTypes.MOVE_FROM_NONPROGRAM_TO_NONPROGAM);
            newEntry.setGroup(firstEntry.group);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            newEntry.setCategory(lastEntry.category);
            newEntry.setBlockCount(lastEntry.blockCount);
            newEntry.setSpritename(lastEntry.spritename);
            newEntry.setBlockID(lastEntry.blockId);
            newEntry.setNewParentId(lastEntry.newParentId);
            newEntry.setNewInputName(lastEntry.newInputName);
            if (lastEntry.conditionalInvolved) newEntry.setConditionalInvolved("true");
            if (lastEntry.loopInvolved) newEntry.setLoopInvolved("true");
            if (groupEntries.size() == 1) {
                newEntry.setMetadata(groupEntries.size() + " event");
            } else {
                newEntry.setMetadata(groupEntries.size() + " events");
            }
            return newEntry;
        }
        else if (firstEntry.event.equals("MOVE") && lastEntry.event.equals("MOVE") && firstEntry.blockId.equals(lastEntry.blockId) && firstEntry.scriptId.equals("") && lastEntry.newParentId.equals("")) {
            WriteEntry newEntry = new WriteEntry();
            //Detaching from Nonprogram
            newEntry.setActionType(ActionTypes.MOVE_FROM_NONPROGRAM_TO_WORKSPACE);
            newEntry.setGroup(firstEntry.group);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            newEntry.setCategory(firstEntry.category);
            newEntry.setBlockCount(firstEntry.blockCount);
            newEntry.setSpritename(firstEntry.spritename);
            newEntry.setOldParentId(firstEntry.oldParentId);
            newEntry.setOldInputName(firstEntry.oldInputName);
            newEntry.setBlockID(firstEntry.blockId);
            if (firstEntry.conditionalInvolved) newEntry.setConditionalInvolved("true");
            if (firstEntry.loopInvolved) newEntry.setLoopInvolved("true");
            if (groupEntries.size() == 1) {
                newEntry.setMetadata(groupEntries.size() + " event");
            } else {
                newEntry.setMetadata(groupEntries.size() + " events");
            }
            return newEntry;
        }
        else if (firstEntry.event.equals("CREATE") && lastEntry.event.equals("DELETE") && firstEntry.blockId.equals(lastEntry.blockId)) {
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.IMMEDIATE_DELETE);
            newEntry.setGroup(firstEntry.group);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            newEntry.setCategory(lastEntry.category);
            newEntry.setSpritename(firstEntry.spritename);
            if (groupEntries.size() == 1) {
                newEntry.setMetadata(groupEntries.size() + " event");
            } else {
                newEntry.setMetadata(groupEntries.size() + " events");
            }
            return newEntry;
        }
        else if (firstEntry.event.equals("ENDDRAG") && lastEntry.event.equals("MOVE") && firstEntry.blockId.equals(lastEntry.blockId) && lastEntry.newParentId.equals("")) {
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.MOVE_WITHOUT_CHANGES);
            newEntry.setGroup(firstEntry.group);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            newEntry.setSpritename(firstEntry.spritename);
            if (groupEntries.size() == 1) {
                newEntry.setMetadata(groupEntries.size() + " event");
            } else {
                newEntry.setMetadata(groupEntries.size() + " events");
            }
            return newEntry;
        }
        else if (firstEntry.event.equals("CHANGE") && lastEntry.event.equals("CHANGE") && !lastEntry.scriptId.equals("")) {
            WriteEntry newEntry = new WriteEntry();
            newEntry.setMetadata(Integer.toString(groupEntries.size()));
            newEntry.setActionType(ActionTypes.CHANGE_DIRECT_VALUE_IN_PROGRAM);
            newEntry.setGroup(firstEntry.group);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            newEntry.setBlockID(firstEntry.blockId);
            newEntry.setOldValue(firstEntry.oldValue);
            newEntry.setNewValue(lastEntry.newValue);
            newEntry.setSpritename(firstEntry.spritename);
            newEntry.setCategory(firstEntry.category);
            newEntry.setValueType(firstEntry.valueType);
            if (firstEntry.conditionalInvolved) newEntry.setConditionalInvolved("true");
            if (firstEntry.loopInvolved) newEntry.setLoopInvolved("true");
            newEntry.setScriptId(firstEntry.scriptId);
            if (groupEntries.size() == 1) {
                newEntry.setMetadata(groupEntries.size() + " event");
            } else {
                newEntry.setMetadata(groupEntries.size() + " events");
            }
            return newEntry;
        }
        else if (firstEntry.event.equals("CHANGE") && lastEntry.event.equals("CHANGE") && lastEntry.scriptId.equals("")) {
            WriteEntry newEntry = new WriteEntry();
            newEntry.setMetadata(Integer.toString(groupEntries.size()));
            newEntry.setActionType(ActionTypes.CHANGE_DIRECT_VALUE_IN_NONPROGRAM);
            newEntry.setGroup(firstEntry.group);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            newEntry.setBlockID(firstEntry.blockId);
            newEntry.setOldValue(firstEntry.oldValue);
            newEntry.setNewValue(lastEntry.newValue);
            newEntry.setSpritename(firstEntry.spritename);
            newEntry.setCategory(firstEntry.category);
            newEntry.setValueType(firstEntry.valueType);
            if (firstEntry.conditionalInvolved) newEntry.setConditionalInvolved("true");
            if (firstEntry.loopInvolved) newEntry.setLoopInvolved("true");
            if (groupEntries.size() == 1) {
                newEntry.setMetadata(groupEntries.size() + " event");
            } else {
                newEntry.setMetadata(groupEntries.size() + " events");
            }
            return newEntry;
        }
        else if (firstEntry.event.equals("MOVE") && lastEntry.event.equals("DELETE") && !firstEntry.scriptId.equals("") && firstEntry.blockId.equals(lastEntry.blockId)) {
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.REMOVE_FROM_PROGRAM_AND_DELETE);
            newEntry.setGroup(firstEntry.group);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            newEntry.setBlockID(firstEntry.blockId);
            newEntry.setSpritename(firstEntry.spritename);
            newEntry.setCategory(firstEntry.category);
            if (firstEntry.conditionalInvolved) newEntry.setConditionalInvolved("true");
            if (firstEntry.loopInvolved) newEntry.setLoopInvolved("true");
            newEntry.setScriptId(firstEntry.scriptId);
            newEntry.setOldInputName(firstEntry.oldInputName);
            newEntry.setOldParentId(firstEntry.oldParentId);
            newEntry.setBlockCount(firstEntry.blockCount);
            if (groupEntries.size() == 1) {
                newEntry.setMetadata(groupEntries.size() + " event");
            } else {
                newEntry.setMetadata(groupEntries.size() + " events");
            }
            return newEntry;
        }
        else if (firstEntry.event.equals("ENDDRAG") && secondEntry.event.equals("MOVE") && lastEntry.event.equals("DELETE") && !secondEntry.scriptId.equals("") && secondEntry.blockId.equals(secondEntry.scriptId)) {
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.REMOVE_AND_DELETE_SCRIPT);
            newEntry.setGroup(firstEntry.group);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            newEntry.setBlockID(firstEntry.blockId);
            newEntry.setSpritename(firstEntry.spritename);
            newEntry.setBlockCount(firstEntry.blockCount);
            if (groupEntries.size() == 1) {
                newEntry.setMetadata(groupEntries.size() + " event");
            } else {
                newEntry.setMetadata(groupEntries.size() + " events");
            }
            return newEntry;
        }
        else if (firstEntry.event.equals("MOVE") && lastEntry.event.equals("DELETE") && firstEntry.scriptId.equals("") && firstEntry.blockId.equals(lastEntry.blockId)) {
            WriteEntry newEntry = new WriteEntry();
            //detached from nonprogram
            newEntry.setActionType(ActionTypes.REMOVE_FROM_NONPROGRAM_AND_DELETE);
            newEntry.setGroup(firstEntry.group);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            newEntry.setBlockID(firstEntry.blockId);
            newEntry.setSpritename(firstEntry.spritename);
            newEntry.setCategory(firstEntry.category);
            if (firstEntry.conditionalInvolved) newEntry.setConditionalInvolved("true");
            if (firstEntry.loopInvolved) newEntry.setLoopInvolved("true");
            newEntry.setOldInputName(firstEntry.oldInputName);
            newEntry.setOldParentId(firstEntry.oldParentId);
            newEntry.setBlockCount(firstEntry.blockCount);
            if (groupEntries.size() == 1) {
                newEntry.setMetadata(groupEntries.size() + " event");
            } else {
                newEntry.setMetadata(groupEntries.size() + " events");
            }
            return newEntry;
        }
        else if (firstEntry.event.equals("ENDDRAG") && lastEntry.event.equals("DELETE") && secondEntry.scriptId.equals("") && firstEntry.blockId.equals(lastEntry.blockId)) {
            WriteEntry newEntry = new WriteEntry();
            //no detaching from nonprogram happened
            newEntry.setActionType(ActionTypes.REMOVE_FROM_NONPROGRAM_AND_DELETE);
            newEntry.setGroup(firstEntry.group);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            newEntry.setBlockID(secondEntry.blockId);
            newEntry.setSpritename(firstEntry.spritename);
            newEntry.setCategory(secondEntry.category);
            newEntry.setBlockCount(secondEntry.blockCount);
            if (groupEntries.size() == 1) {
                newEntry.setMetadata(groupEntries.size() + " event");
            } else {
                newEntry.setMetadata(groupEntries.size() + " events");
            }
            return newEntry;
        }
        else if ((firstEntry.event.equals("CONTEXTMENUBLOCKOPEN") || firstEntry.event.equals("CONTEXTMENUWORKSPACEOPEN")) && secondEntry.event.equals("CONTEXTMENUCLOSEDWITHOUTSELECTION")) {
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.CONTEXTMENU_CLOSED_WITHOUT_SELECTION);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            return newEntry;
        }
        else if (firstEntry.event.equals("CONTEXTMENUBLOCKOPEN") && secondEntry.event.equals("CONTEXTMENUBLOCKDUPLICATE")) {
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.CONTEXTMENU_BLOCK_DUPLICATE);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            return newEntry;
        }
        else if (groupEntries.size() == 5 && firstEntry.event.equals("CONTEXTMENUBLOCKOPEN") && secondEntry.event.equals("CONTEXTMENUBLOCKDELETE") && !groupEntries.get(2).blockId.equals(groupEntries.get(3).blockId) && groupEntries.get(2).event.equals("MOVE") && groupEntries.get(3).event.equals("DELETE")) {
            //Special case where the topmost block of a stack is deleted, this fires a move event detaching its next block followed
            //by a delete event of the block selected to be deleted
            WriteEntry newEntry = new WriteEntry();
            ReadEntry thirdEntry = groupEntries.get(2);
            ReadEntry fourthEntry = groupEntries.get(3);
            if (!thirdEntry.scriptId.equals("")) {
                newEntry.setActionType(ActionTypes.CONTEXTMENU_BLOCK_DELETE_FROM_PROGRAM);
            } else {
                newEntry.setActionType(ActionTypes.CONTEXTMENU_BLOCK_DELETE_FROM_NONPROGRAM);
            }
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            newEntry.setBlockID(fourthEntry.blockId);
            newEntry.setSpritename(fourthEntry.spritename);
            newEntry.setBlockCount(fourthEntry.blockCount);
            newEntry.setCategory(fourthEntry.category);
            newEntry.setScriptId(thirdEntry.scriptId);
            if (groupEntries.size() == 1) {
                newEntry.setMetadata(groupEntries.size() + " event");
            } else {
                newEntry.setMetadata(groupEntries.size() + " events");
            }
            return newEntry;
        }
        else if (firstEntry.event.equals("CONTEXTMENUBLOCKOPEN") && secondEntry.event.equals("CONTEXTMENUBLOCKDELETE")) {
            WriteEntry newEntry = new WriteEntry();
            ReadEntry thirdEntry = groupEntries.get(2);
            if (!thirdEntry.scriptId.equals("")) {
                newEntry.setActionType(ActionTypes.CONTEXTMENU_BLOCK_DELETE_FROM_PROGRAM);
            } else {
                newEntry.setActionType(ActionTypes.CONTEXTMENU_BLOCK_DELETE_FROM_NONPROGRAM);
            }
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            newEntry.setBlockID(thirdEntry.blockId);
            newEntry.setSpritename(thirdEntry.spritename);
            newEntry.setBlockCount(thirdEntry.blockCount);
            newEntry.setGroup(secondToLastEntry.group);
            newEntry.setScriptId(thirdEntry.scriptId);
            newEntry.setCategory(thirdEntry.category);
            newEntry.setOldParentId(thirdEntry.oldParentId);
            newEntry.setOldInputName(thirdEntry.oldInputName);
            if (thirdEntry.loopInvolved) newEntry.setLoopInvolved("true");
            if (thirdEntry.conditionalInvolved) newEntry.setConditionalInvolved("true");
            if (groupEntries.size() == 1) {
                newEntry.setMetadata(groupEntries.size() + " event");
            } else {
                newEntry.setMetadata(groupEntries.size() + " events");
            }
            return newEntry;
        }
        else if (firstEntry.event.equals("CONTEXTMENUBLOCKOPEN") && secondEntry.event.equals("CONTEXTMENUREMOVECOMMENT")) {
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.CONTEXTMENU_BLOCK_REMOVE_COMMENT);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            return newEntry;
        }
        else if (firstEntry.event.equals("CONTEXTMENUBLOCKOPEN") && secondEntry.event.equals("CONTEXTMENUBLOCKADDCOMMENT")) {
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.CONTEXTMENU_BLOCK_ADD_COMMENT);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            return newEntry;
        }
        else if (firstEntry.event.equals("CONTEXTMENUWORKSPACEOPEN") && secondEntry.event.equals("CONTEXTMENUWORKSPACEADDCOMMENT")) {
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.CONTEXTMENU_WORKSPACE_ADD_COMMENT);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            return newEntry;
        }
        else if (firstEntry.event.equals("CONTEXTMENUWORKSPACEOPEN") && secondEntry.event.equals("CONTEXTMENUWORKSPACEDELETEBLOCKS")) {
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.CONTEXTMENU_WORKSPACE_DELETE_BLOCKS);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            if (groupEntries.size() == 1) {
                newEntry.setMetadata(groupEntries.size() + " event");
            } else {
                newEntry.setMetadata(groupEntries.size() + " events");
            }
            return newEntry;
        }
        else if (firstEntry.event.equals("CONTEXTMENUWORKSPACEOPEN") && secondEntry.event.equals("CONTEXTMENUWORKSPACECLEANUP")) {
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.CONTEXTMENU_WORKSPACE_CLEANUP);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            return newEntry;
        }
        else if (firstEntry.event.equals("CONTEXTMENUWORKSPACEOPEN") && secondEntry.event.equals("CONTEXTMENUWORKSPACEUNDO")) {
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.CONTEXTMENU_WORKSPACE_UNDO);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            return newEntry;
        }
        else if (firstEntry.event.equals("CONTEXTMENUWORKSPACEOPEN") && secondEntry.event.equals("CONTEXTMENUWORKSPACEREDO")) {
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.CONTEXTMENU_WORKSPACE_REDO);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            return newEntry;
        } else if (firstEntry.event.equals("DROPDOWNMENUOPEN") && getCountOfEvent(groupEntries, "DELETE_VAR") > 0) {
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.DROPDOWNMENU_SELECT_DELETE_VAR);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            return newEntry;
        }
        else if (firstEntry.event.equals("DROPDOWNMENUOPEN") && secondEntry.event.equals("DROPDOWNMENUCLOSED")) {
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.DROPDOWNMENU_INSPECT_NO_CHANGES);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            return newEntry;
        }
        else if (firstEntry.event.equals("DROPDOWNMENUOPEN") && secondEntry.event.equals("MOVE") && secondEntry.category.equals("var")) {
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.DROPDOWNENU_SELECT_RENAME_VAR);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            return newEntry;
        }
        else if (groupEntries.size() > 2 && firstEntry.event.equals("DROPDOWNMENUOPEN") && secondEntry.event.equals("CHANGE") && lastEntry.event.equals("DROPDOWNMENUCLOSED")) {
            WriteEntry newEntry = new WriteEntry();
            if (!secondEntry.scriptId.equals("")) {
                newEntry.setActionType(ActionTypes.DROPDOWNMENU_CHANGE_VALUE_IN_PROGRAM);
            } else {
                newEntry.setActionType(ActionTypes.DROPDOWNMENU_CHANGE_VALUE_IN_NONPROGRAM);
            }
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(lastEntry.date);
            newEntry.setBlockID(secondEntry.blockId);
            newEntry.setScriptId(secondEntry.scriptId);
            if (secondEntry.loopInvolved) newEntry.setLoopInvolved("true");
            if (secondEntry.conditionalInvolved) newEntry.setConditionalInvolved("true");
            newEntry.setOldValue(secondEntry.oldValue);
            newEntry.setNewValue(secondToLastEntry.newValue);
            newEntry.setValueType(secondEntry.valueType);
            newEntry.setCategory(secondEntry.category);
            return newEntry;
        }

        else {
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.UNCAUGHT_ACTION_TYPE);
            newEntry.setBegin(firstEntry.date);
            newEntry.setEnd(firstEntry.date);
            newEntry.setGroup(firstEntry.group);
            newEntry.setMetadata(groupEntries.size() + " events");
            return newEntry;
        }
    }

    public static WriteEntry handleSingleEntry(ReadEntry readEntry) {
        if (readEntry.event.equals("STACKCLICK") && !readEntry.scriptId.equals("")) {
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.CLICK_PROGRAM);
            newEntry.setBegin(readEntry.date);
            newEntry.setEnd(readEntry.date);
            newEntry.setScriptId(readEntry.scriptId);
            newEntry.setBlockID(readEntry.blockId);
            newEntry.setSpritename(readEntry.spritename);
            return newEntry;
        } else if (readEntry.event.equals("STACKCLICK") && readEntry.scriptId.equals("")) {
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.CLICK_NONPROGRAM);
            newEntry.setBegin(readEntry.date);
            newEntry.setBlockID(readEntry.blockId);
            newEntry.setEnd(readEntry.date);
            newEntry.setSpritename(readEntry.spritename);
            return newEntry;
        } else if (readEntry.event.equals("DELETE") && !readEntry.group.equals("")) {
            //The backspace key can be used to delete a block causing a singular event
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.DELETE_BLOCK_THROUGH_BACKSPACE);
            newEntry.setBegin(readEntry.date);
            newEntry.setEnd(readEntry.date);
            newEntry.setBlockID(readEntry.blockId);
            newEntry.setCategory(readEntry.category);
            newEntry.setSpritename(readEntry.spritename);
            newEntry.setBlockCount(readEntry.blockCount);
            return newEntry;
        } else if (readEntry.event.equals("DELETE") && readEntry.group.equals("")) {
            //Singular delete events that are automatically fired, for example when creating and moving a block outside
            //the workspace in one movement
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.AUTOMATIC_DELETE);
            newEntry.setBegin(readEntry.date);
            newEntry.setEnd(readEntry.date);
            newEntry.setBlockID(readEntry.blockId);
            newEntry.setCategory(readEntry.category);
            newEntry.setSpritename(readEntry.spritename);
            newEntry.setBlockCount(readEntry.blockCount);
            return newEntry;
        } else if (readEntry.event.equals("CHANGE") && !readEntry.scriptId.equals("")) {
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.CHANGE_DIRECT_VALUE_IN_PROGRAM);
            newEntry.setBegin(readEntry.date);
            newEntry.setEnd(readEntry.date);
            newEntry.setBlockID(readEntry.blockId);
            newEntry.setOldValue(readEntry.oldValue);
            newEntry.setNewValue(readEntry.newValue);
            newEntry.setSpritename(readEntry.spritename);
            newEntry.setCategory(readEntry.category);
            if (readEntry.conditionalInvolved) newEntry.setConditionalInvolved("true");
            if (readEntry.loopInvolved) newEntry.setLoopInvolved("true");
            newEntry.setScriptId(readEntry.scriptId);
            return newEntry;
        } else if (readEntry.event.equals("CHANGE") && readEntry.scriptId.equals("")) {
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.CHANGE_DIRECT_VALUE_IN_NONPROGRAM);
            newEntry.setBegin(readEntry.date);
            newEntry.setEnd(readEntry.date);
            newEntry.setBlockID(readEntry.blockId);
            newEntry.setOldValue(readEntry.oldValue);
            newEntry.setNewValue(readEntry.newValue);
            newEntry.setSpritename(readEntry.spritename);
            newEntry.setCategory(readEntry.category);
            if (readEntry.conditionalInvolved) newEntry.setConditionalInvolved("true");
            if (readEntry.loopInvolved) newEntry.setLoopInvolved("true");
            return newEntry;
        } else if (readEntry.event.equals("SPRITESWITCH")) {
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.SPRITE_SWITCH);
            newEntry.setBegin(readEntry.date);
            newEntry.setEnd(readEntry.date);
            newEntry.setSpritename(readEntry.metadata);
            return newEntry;
        } else if (readEntry.event.equals("MOVE")) {
            //The event was caused because the move ended outside the workspace, reverts the preceding move
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.AUTOMATIC_MOVE_BACK);
            newEntry.setBegin(readEntry.date);
            newEntry.setEnd(readEntry.date);
            newEntry.setSpritename(readEntry.getSpritename());
            newEntry.setBlockID(readEntry.blockId);
            newEntry.setBlockCount(readEntry.blockCount);
            if (readEntry.loopInvolved) newEntry.setLoopInvolved("true");
            if (readEntry.conditionalInvolved) newEntry.setConditionalInvolved("true");
            newEntry.setScriptIdAfterMove(readEntry.scriptIdAfterMove);
            newEntry.setGroup(readEntry.group);
            return newEntry;
        } else if (readEntry.event.equals("CREATE")) {
            //The event was caused by duplicating a block; since no contextmenu-end event follows, this must be handled
            //seperately
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.BLOCK_DUPLICATED);
            newEntry.setBegin(readEntry.date);
            newEntry.setEnd(readEntry.date);
            newEntry.setSpritename(readEntry.getSpritename());
            newEntry.setBlockID(readEntry.blockId);
            newEntry.setBlockCount(readEntry.blockCount);
            newEntry.setGroup(readEntry.group);
            return newEntry;
        } else if (readEntry.event.equals("GREENFLAG") && readEntry.metadata.equals("green-flag_green-flag_mk1Vo")) {
            //The program is not running
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.CLICK_GREENFLAG_PROGRAM_NOT_RUNNING);
            newEntry.setBegin(readEntry.date);
            newEntry.setEnd(readEntry.date);
            return newEntry;
        } else if (readEntry.event.equals("GREENFLAG")) {
            //The program is running
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.CLICK_GREENFLAG_PROGRAM_RUNNING);
            newEntry.setBegin(readEntry.date);
            newEntry.setEnd(readEntry.date);
            return newEntry;
        } else if (readEntry.event.equals("STOPALL")) {
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.CLICK_STOP_ALL);
            newEntry.setBegin(readEntry.date);
            newEntry.setEnd(readEntry.date);
            return newEntry;
        } else if (readEntry.event.equals("INACTIVITYPERIODENDED")) {
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.INACTIVITY_PERIOD_ENDED);
            newEntry.setEnd(readEntry.date);
            newEntry.setMetadata(readEntry.metadata.concat(" ms"));
            newEntry.setGroup(readEntry.group);
            return newEntry;
        } else if (readEntry.event.equals("COMMENT_OPEN")) {
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.COMMENT_OPEN);
            newEntry.setEnd(readEntry.date);
            newEntry.setSpritename(readEntry.spritename);
            newEntry.setGroup(readEntry.group);
            return newEntry;
        } else if (readEntry.event.equals("COMMENT_CLOSE")) {
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.COMMENT_CLOSE);
            newEntry.setEnd(readEntry.date);
            newEntry.setSpritename(readEntry.spritename);
            newEntry.setGroup(readEntry.group);
            return newEntry;
        } else if (readEntry.event.equals("COMMENT_MOVE")) {
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.COMMENT_MOVE);
            newEntry.setEnd(readEntry.date);
            newEntry.setSpritename(readEntry.spritename);
            newEntry.setGroup(readEntry.group);
            return newEntry;
        } else if (readEntry.event.equals("CREATE_VAR_GLOBAL") || readEntry.event.equals("CREATE_VAR_LOCAL")) {
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.CREATE_VAR);
            newEntry.setEnd(readEntry.date);
            newEntry.setMetadata("Var name: ".concat(readEntry.metadata));
            newEntry.setSpritename(readEntry.spritename);
            newEntry.setGroup(readEntry.group);
            return newEntry;
        } else if (readEntry.event.equals("DELETE_VAR")) {
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.DELETE_VAR);
            newEntry.setEnd(readEntry.date);
            newEntry.setMetadata("Var name: ".concat(readEntry.metadata));
            newEntry.setSpritename(readEntry.spritename);
            newEntry.setGroup(readEntry.group);
            newEntry.setGroup(readEntry.group);
            return newEntry;
        }

        else {
            WriteEntry newEntry = new WriteEntry();
            newEntry.setActionType(ActionTypes.UNCAUGHT_ACTION_TYPE);
            newEntry.setMetadata("1 Entry");
            return newEntry;
        }
    }

    //Combines all entries with identical group attribute into one list
    //This is also done for all sequences beginning with a CONTEXTMENUOPEN/CONTEXTMENUWORKSPACEOPEN event and ending with
    //a CONTEXTMENUEVENTEND, CONTEXTMENUCLOSEDWITHOUTSELECTION, CONTEXTMENUBLOCKDUPLICATE, CONTEXTMENUBLOCKADDCOMMENT
    //or CONTEXTMENUBLOCKREMOVECOMMENT event
    //or beginning with a DROPDOWNMENUOPEN event and ending with a DROPDOWNMENUCLOSED event
    public static List<List<ReadEntry>> combineSameGroupEntries(List<ReadEntry> readEntryList) {
        List<List<ReadEntry>> returnList = new ArrayList<>();
        boolean contextmenuCurrent = false;
        boolean dropdownMenuCurrent = false;
        for (int i = 0; i < readEntryList.size(); i++) {
            List<ReadEntry> newEntry = new ArrayList<>();
            newEntry.add(readEntryList.get(i));
            if (readEntryList.get(i).event.equals("CONTEXTMENUBLOCKOPEN") || readEntryList.get(i).event.equals("CONTEXTMENUWORKSPACEOPEN")) {
                contextmenuCurrent = true;
            }
            if (readEntryList.get(i).event.equals("DROPDOWNMENUOPEN")) {
                dropdownMenuCurrent = true;
            }
            while (i < readEntryList.size() - 1) {
                if (contextmenuCurrent) {
                    if (!readEntryList.get(i+1).event.equals("CONTEXTMENUEVENTEND") && !readEntryList.get(i+1).event.equals("CONTEXTMENUCLOSEDWITHOUTSELECTION") && !readEntryList.get(i+1).event.equals("CONTEXTMENUBLOCKDUPLICATE") && !readEntryList.get(i+1).event.equals("CONTEXTMENUBLOCKADDCOMMENT") && !readEntryList.get(i+1).event.equals("CONTEXTMENUBLOCKREMOVECOMMENT")) {
                        newEntry.add(readEntryList.get(i+1));
                        i++;
                    } else {
                        newEntry.add(readEntryList.get(i+1));
                        i++;
                        contextmenuCurrent = false;
                    }
                } else if (dropdownMenuCurrent) {
                    if (!readEntryList.get(i+1).event.equals("DROPDOWNMENUCLOSED")) {
                        newEntry.add(readEntryList.get(i+1));
                        i++;
                    } else {
                        newEntry.add(readEntryList.get(i+1));
                        i++;
                        dropdownMenuCurrent = false;
                    }
                } else if (readEntryList.get(i).group.equals(readEntryList.get(i + 1).group) && !readEntryList.get(i).group.equals("")) {
                    newEntry.add(readEntryList.get(i+1));
                    i++;
                } else if (readEntryList.get(i).event.equals(readEntryList.get(i + 1).event) && readEntryList.get(i).event.equals("CHANGE") && readEntryList.get(i).blockId.equals(readEntryList.get(i + 1).blockId)) {
                    newEntry.add(readEntryList.get(i + 1));
                    i++;
                } else {
                    break;
                }
            }
            returnList.add(newEntry);
        }
        return returnList;
    }

    public static int getCountOfEvent(List<ReadEntry> list, String event) {
        int count = 0;
        for (ReadEntry entry: list) {
            if (event.equals(entry.event)) {
                count++;
            }
        }
        return count;
    }

    public static ReadEntry getfirstEntryWithEvent(List<ReadEntry> list, String event) {
        ReadEntry readEntry = null;
        for (ReadEntry entry: list) {
            if (event.equals(entry.event)) {
                readEntry = entry;
                break;
            }
        }
        return readEntry;
    }

    public static int getCountOfSetNewParentIds(List<ReadEntry> list) {
        int count = 0;
        for (ReadEntry entry: list) {
            if (!entry.newParentId.equals("")) {
                count++;
            }
        }
        return count;
    }

    public static void removeMeaninglessMoveEventsAtTheEnd(List<ReadEntry> list) {
        int listSize = list.size();
        //remove move events caused by blocks other than the main block which do not specify a change in parent
        //these events happen when other blocks are pushed aside without changing any block order
        for (int i = listSize - 1; i >= 0; i--) {
            ReadEntry entry = list.get(i);
            if (entry.event.equals("MOVE") && !entry.blockId.equals(list.get(0).blockId) && entry.newParentId.equals("") && entry.oldParentId.equals("")) {
                list.remove(entry);
            } else {
                break;
            }
        }
    }

    public static void removeInactivityEventsInList(List<ReadEntry> list) {
        int listSize = list.size();
        //remove InactivityPeriodEnded events in actions with multiple entries since this could otherwise break the parsing
        for (int i = listSize - 1; i >= 0; i--) {
            ReadEntry entry = list.get(i);
            if (entry.event.equals("INACTIVITYPERIODENDED")) {
                list.remove(entry);
            }
        }
    }


    //Writes the parsed data into the output file
    public static void writeDataAtOnce(String filePath, List<String[]> writeData)
    {

        // first create file object for file placed at location
        // specified by filepath
        File file = new File(filePath);

        try {
            // create FileWriter object with file as parameter
            FileWriter outputfile = new FileWriter(file);

            // create CSVWriter object filewriter object as parameter
            CSVWriter writer = new CSVWriter(outputfile);

            writer.writeAll(writeData);

            // closing writer connection
            writer.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}