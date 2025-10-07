package org.example;

public enum ActionTypes {
    //Create a hat block and add it to the workspace
    ADD_HAT_TO_WORKSPACE,
    //Create a hat block and put it on top of a block(stack)
    ADD_HAT_TO_NONPROGRAM,
    //Create a stackblock and instantly attach it to a block belonging to the program
    ADD_NEW_BLOCK_TO_PROGRAM,
    //Create a stackblock and instantly attach it to a block not belonging to the program
    ADD_NEW_BLOCK_TO_NONPROGRAM,
    //Create a stackblock and place it into the workspace
    ADD_NEW_BLOCK_TO_WORKSPACE,
    //Attach an existing block to a block belonging to the program
    MOVE_FROM_PROGRAM_TO_PROGAM,
    //Move a block(stack) from inside the program to outside the program (but connected to other blocks outside the program)
    MOVE_FROM_PROGRAM_TO_NONPROGRAM,
    //Move a block(stack) from inside the program to the workspace
    MOVE_FROM_PROGRAM_TO_WORKSPACE,
    //Move a block(stack) from outside the program to inside the program
    MOVE_FROM_NONPROGRAM_TO_PROGAM,
    //Move a block(stack) from outside the program to still outside the program (but connected to other blocks outside the program)
    MOVE_FROM_NONPROGRAM_TO_NONPROGAM,
    //Move a block(stack) from outside the program to the workspace
    MOVE_FROM_NONPROGRAM_TO_WORKSPACE,
    //Movement that has no effect on the program
    MOVE_WITHOUT_CHANGES,
    CHANGE_DIRECT_VALUE_IN_PROGRAM,
    CHANGE_DIRECT_VALUE_IN_NONPROGRAM,
    IMMEDIATE_DELETE,
    CLICK_PROGRAM,
    CLICK_NONPROGRAM,
    SPRITE_SWITCH,
    AUTOMATIC_MOVE_BACK,
    AUTOMATIC_DELETE,
    BLOCK_DUPLICATED,
    REMOVE_FROM_PROGRAM_AND_DELETE,
    REMOVE_AND_DELETE_SCRIPT,
    REMOVE_FROM_NONPROGRAM_AND_DELETE,
    CLICK_GREENFLAG_PROGRAM_RUNNING,
    CLICK_GREENFLAG_PROGRAM_NOT_RUNNING,
    CLICK_STOP_ALL,
    CONTEXTMENU_BLOCK_DUPLICATE,
    CONTEXTMENU_BLOCK_ADD_COMMENT,
    CONTEXTMENU_BLOCK_REMOVE_COMMENT,
    CONTEXTMENU_BLOCK_DELETE_FROM_PROGRAM,
    CONTEXTMENU_BLOCK_DELETE_FROM_NONPROGRAM,
    CONTEXTMENU_CLOSED_WITHOUT_SELECTION,
    CONTEXTMENU_WORKSPACE_ADD_COMMENT,
    CONTEXTMENU_WORKSPACE_DELETE_BLOCKS,
    CONTEXTMENU_WORKSPACE_UNDO,
    CONTEXTMENU_WORKSPACE_REDO,
    CONTEXTMENU_WORKSPACE_CLEANUP,
    DROPDOWNMENU_INSPECT_NO_CHANGES,
    DROPDOWNMENU_CHANGE_VALUE_IN_PROGRAM,
    DROPDOWNMENU_CHANGE_VALUE_IN_NONPROGRAM,

    DROPDOWNENU_SELECT_RENAME_VAR,

    DROPDOWNMENU_SELECT_DELETE_VAR,
    INACTIVITY_PERIOD_ENDED,

    COMMENT_OPEN,

    COMMENT_CLOSE,
    COMMENT_MOVE,

    CREATE_VAR,

    RENAME_VAR,
    DELETE_VAR,

    DELETE_BLOCK_THROUGH_BACKSPACE,
    UNCAUGHT_ACTION_TYPE

}
