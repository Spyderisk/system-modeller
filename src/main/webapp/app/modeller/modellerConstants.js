export const GET_MODEL = "GET_MODEL";
export const GET_MODEL_LOADING_ID = "GET_MODEL_LOADING_ID";
export const GET_PALETTE = "GET_PALETTE";
//export const GET_VALIDATED_MODEL = "GET_VALIDATED_MODEL";
export const RESET_VALIDATION_STATUS = "RESET_VALIDATION_STATUS";
export const POST_MODEL = "POST_MODEL";
export const EDIT_MODEL = "EDIT_MODEL";
export const GET_ASSET = "GET_ASSET";
export const POST_ASSET = "POST_ASSET";
export const PUT_ASSET_NAME = "PUT_ASSET_NAME";
export const PUT_ASSET_TYPE = "PUT_ASSET_TYPE";
export const PUT_ASSET_CARD = "PUT_ASSET_CARD";
export const PUT_ASSET_POPULATION = "PUT_ASSET_POPULATION";
export const PUT_ASSET_LOC = "PUT_ASSET_LOC";
export const UPDATE_ASSET_LOCS = "UPDATE_ASSET_LOCS";
export const DELETE_ASSET = "DELETE_ASSET";
export const GET_RELATION = "GET_RELATION";
export const POST_RELATION = "POST_RELATION";
export const PATCH_RELATION = "PATCH_RELATION";
export const HIDE_RELATION = "HIDE_RELATION";
export const DELETE_RELATION = "DELETE_RELATION";
export const CHANGE_SELECTED_ASSET = "CHANGE_SELECTED_ASSET";
export const CHANGE_SELECTED_TWAS = "CHANGE_SELECTED_TWAS";
export const CHANGE_SELECTED_INFERRED_ASSET = "CHANGE_SELECTED_INFERRED_ASSET";
export const GET_UPDATED_THREATS = "GET_UPDATED_THREATS";
export const PATCH_UPDATED_THREAT = "PATCH_UPDATED_THREAT";
export const GET_CONTROLS = "GET_CONTROLS";
export const PATCH_UPDATED_CONTROL = "PATCH_UPDATED_CONTROL";
export const ACCEPT_THREAT = "ACCEPT_THREAT";
export const GET_COMPILED_ASSET_DETAILS = "GET_COMPILED_ASSET_DETAILS";
export const UPDATE_CONTROLS = "UPDATE_CONTROLS";
export const UPDATE_TWAS = "UPDATE_TWAS";
export const UPDATE_MISBEHAVIOUR_IMPACT = "UPDATE_MISBEHAVIOUR_IMPACT";
export const GET_SECONDARY_EFFECTS = "GET_SECONDARY_EFFECTS";
export const GET_ROOT_CAUSES = "GET_ROOT_CAUSES";
export const GET_CAUSE_EFFECT = "GET_CAUSE_EFFECT";
export const TOGGLE_THREAT_EDITOR = "TOGGLE_THREAT_EDITOR";
export const TOGGLE_PANEL = "TOGGLE_PANEL";
export const TOGGLE_FILTER = "TOGGLE_FILTER";
export const ACTIVATE_THREAT_EDITOR = "ACTIVATE_THREAT_EDITOR";
export const ACTIVATE_MISBEHAVIOUR_EXPLORER = "ACTIVATE_MISBEHAVIOUR_EXPLORER";
export const ACTIVATE_COMPLIANCE_EXPLORER = "ACTIVATE_COMPLIANCE_EXPLORER";
export const ACTIVATE_CONTROL_EXPLORER = "ACTIVATE_CONTROL_EXPLORER";
export const ACTIVATE_THREAT_FILTER = "ACTIVATE_THREAT_FILTER";
export const ACTIVATE_ACCEPTANCE_PANEL = "ACTIVATE_ACCEPTANCE_PANEL";
export const SIDE_PANEL_WIDTHS = "SIDE_PANEL_WIDTHS";
export const SIDE_PANEL_ACTIVATED = "SIDE_PANEL_ACTIVATED";
export const SIDE_PANEL_DEACTIVATED = "SIDE_PANEL_DEACTIVATED";
export const OPEN_REPORT_DIALOG = "OPEN_REPORT_DIALOG";
export const CLOSE_REPORT_DIALOG = "CLOSE_REPORT_DIALOG";
export const CLOSE_MISBEHAVIOUR_EXPLORER = "CLOSE_MISBEHAVIOUR_EXPLORER";
export const OPEN_COMPLIANCE_EXPLORER = "OPEN_COMPLIANCE_EXPLORER";
export const OPEN_CONTROL_EXPLORER = "OPEN_CONTROL_EXPLORER";
export const OPEN_CONTROL_STRATEGY_EXPLORER = "OPEN_CONTROL_STRATEGY_EXPLORER";
export const OPEN_RECOMMENDATIONS_EXPLORER = "OPEN_RECOMMENDATIONS_EXPLORER";
export const CLOSE_COMPLIANCE_EXPLORER = "CLOSE_COMPLIANCE_EXPLORER";
export const CLOSE_CONTROL_EXPLORER = "CLOSE_CONTROL_EXPLORER";
export const CLOSE_CONTROL_STRATEGY_EXPLORER = "CLOSE_CONTROL_STRATEGY_EXPLORER";
export const CLOSE_RECOMMENDATIONS_EXPLORER = "CLOSE_RECOMMENDATIONS_EXPLORER";
export const SUPPRESS_CANVAS_REFRESH = "SUPPRESS_CANVAS_REFRESH";
export const REDRAW_RELATIONS = "REDRAW_RELATIONS";
export const HOVER_THREAT = "HOVER_THREAT";
export const UPLOAD_SCREENSHOT = "UPLOAD_SCREENSHOT";
export const UPLOAD_MODEL = "UPLOAD_MODEL";
export const UPDATE_UPLOAD_PROGRESS = "UPDATE_UPLOAD_PROGRESS";
export const UPDATE_LAYER_SELECTION = "UPDATE_LAYER_SELECTION";
export const IS_VALIDATING = "IS_VALIDATING";
export const IS_NOT_VALIDATING = "IS_NOT_VALIDATING";
export const IS_CALCULATING_RISKS = "IS_CALCULATING_RISKS";
export const IS_NOT_CALCULATING_RISKS = "IS_NOT_CALCULATING_RISKS";
export const IS_CALCULATING_RECOMMENDATIONS = "IS_CALCULATING_RECOMMENDATIONS";
export const IS_NOT_CALCULATING_RECOMMENDATIONS = "IS_NOT_CALCULATING_RECOMMENDATIONS";
export const IS_DROPPING_INFERRED_GRAPH = "IS_DROPPING_INFERRED_GRAPH";
export const IS_NOT_DROPPING_INFERRED_GRAPH = "IS_NOT_DROPPING_INFERRED_GRAPH";
export const RISK_CALC_RESULTS = "RISK_CALC_RESULTS";
export const RECOMMENDATIONS_JOB_STARTED = "RECOMMENDATIONS_JOB_STARTED";
export const RECOMMENDATIONS_RESULTS = "RECOMMENDATIONS_RESULTS";
export const VALIDATION_FAILED = "VALIDATION_FAILED";
export const RISK_CALC_FAILED = "RISK_CALC_FAILED";
export const RECOMMENDATIONS_FAILED = "RECOMMENDATIONS_FAILED";
export const RESOLVE_RELATION_ISSUE = "RESOLVE_RELATION_ISSUE";
export const GET_ISSUES = "GET_ISSUES";
export const UPDATE_VALIDATION_PROGRESS = "UPDATE_VALIDATION_PROGRESS";
export const UPDATE_RISK_CALC_PROGRESS = "UPDATE_RISK_CALC_PROGRESS";
export const UPDATE_RECOMMENDATIONS_PROGRESS = "UPDATE_RECOMMENDATIONS_PROGRESS";
export const UPDATE_LOADING_PROGRESS = "UPDATE_LOADING_PROGRESS";
export const UPDATE_THREAT_LOADING = "UPDATE_THREAT_LOADING";
export const UPDATE_DETAILS_LOADING = "UPDATE_DETAILS_LOADING";
export const UPDATE_MODEL_LOADING = "UPDATE_MODEL_LOADING";
export const UPDATE_CARD_LOADING = "UPDATE_CARD_LOADING";
export const UPDATE_ASSET_LOADING = "UPDATE_ASSET_LOADING";
export const NEW_FACT = "NEW_FACT";
export const SET_ZOOM = "SET_ZOOM";
export const SET_TRANSFORM_ORIGIN = "SET_TRANSFORM_ORIGIN";
export const SHOW_INFERRED_RELATIONS = "SHOW_INFERRED_RELATIONS";
export const SHOW_HIDDEN_RELATIONS = "SHOW_HIDDEN_RELATIONS";
export const RECENTRE_MODEL = "RECENTRE_MODEL";
export const RECENTRE_CANVAS = "RECENTRE_CANVAS";
export const ASSET_HISTORY_FORWARD = "ASSET_HISTORY_FORWARD";
export const ASSET_HISTORY_BACK = "ASSET_HISTORY_BACK";
export const TOGGLE_DEVELOPER_MODE = "TOGGLE_DEVELOPER_MODE";

// view reducer constants
export const OPEN_WINDOW = "OPEN_WINDOW";
export const CLOSE_WINDOW = "CLOSE_WINDOW";

// interaction reducer constants
export const INTERACTION_ADD_ASSET = "INTERACTION_ADD_ASSET";
export const INTERACTION_COPY_ASSET = "INTERACTION_COPY_ASSET";
export const INTERACTION_CLEAR_CLIPBOARD = "INTERACTION_CLEAR_CLIPBOARD";
export const INTERACTION_COPY_RELATION = "INTERACTION_COPY_RELATION";
export const INTERACTION_ADD_RELATION = "INTERACTION_ADD_RELATION";

// additional properties constants
export const GET_METADATA = "GET_METADATA";
export const PUT_METADATA = "PUT_METADATA";

// authz
export const UPDATE_AUTHZ = "UPDATE_AUTHZ";
export const GET_AUTHZ = "GET_AUTHZ";
export const UPDATE_EDIT = "UPDATE_EDIT";

// asset group constants
export const POST_GROUP = "POST_GROUP"
export const PUT_GROUP_ADD_ASSET = "PUT_GROUP_ADD_ASSET"
export const PUT_GROUP_REMOVE_ASSET = "PUT_GROUP_REMOVE_ASSET"
export const PUT_GROUP_RENAME = "PUT_GROUP_RENAME"
export const PUT_GROUP_RESIZE = "PUT_GROUP_RESIZE"
export const PUT_GROUP_LOC = "PUT_GROUP_LOC"
export const PUT_GROUP_EXPANDED = "PUT_GROUP_EXPANDED"
export const SET_GROUP_RESIZABLE = "SET_GROUP_RESIZABLE"
export const MOVE_ASSET_TO_GROUP = "MOVE_ASSET_TO_GROUP"
export const DELETE_GROUP = "DELETE_GROUP"
export const GROUPING = "GROUPING"
export const MOVING_ASSET = "MOVING_ASSET"
export const MOVING_GROUP = "MOVING_GROUP"

// other
export const PRINT_MODEL_STATE = "PRINT_MODEL_STATE"

// open graph window
export const OPEN_GRAPH_WINDOW = "OPEN_GRAPH_WINDOW"

// attack path
export const LOADING_ATTACK_PATH = "LOADING_ATTACK_PATH"
export const GET_ATTACK_PATH = "GET_ATTACK_PATH"
