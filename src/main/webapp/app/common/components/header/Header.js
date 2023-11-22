import PropTypes from "prop-types";
import React, { Fragment } from "react";
import {
    DropdownButton,
    MenuItem,
    OverlayTrigger, Tooltip
} from "react-bootstrap";
import { connect } from "react-redux";
import { saveDownload } from "../../../common/actions/api";
import * as Constants from "../../../common/constants.js";
import { openDocumentation, openApiDocs } from "../../../common/documentation/documentation";
import {
    reCentreCanvas,
    reCentreModel,
    showHiddenRelations,
    showInferredRelations,
    sidePanelActivated
} from "../../../modeller/actions/ModellerActions";
import { getUser } from "../../actions/api";
import * as actions from "../../reducers/auth";
import { loadWelcomePage } from "../../rest/rest";
import Link from "../link/Link";
import LoadPageMenuItem from "../link/LoadPageMenuItem";
import "./Header.scss";

class Header extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            exportDropdownOpen: false,
            viewSettingsOpen: false,
            showInferredRelations: false,
            showHiddenRelations: false,
            keepSessionAlive: {
                confirmedValidSession: false,
                timerId: null,
            },
        };

        this.bindEventHandlers = this.bindEventHandlers.bind(this);
    }

    componentWillMount() {
        // Keep the user's session alive while the user is interacting
        // with the UI. This is only really important when on the modeller
        // page as the REST endpoints can be accessed without authentication.
        // This means the user's session may expire. To prevent this we
        // periodically "ping the server" to keep the session alive.
        this.startKeepSessionAlive();

        // We allow the session to expire if the window is not visible,
        // as the user clearly isn't interacting with the UI. However,
        // we don't want a brief switch to another window to result in
        // the session expiring, so we restart pinging the server
        // when the window becomes visble again. This will not create
        // a new session if the session has already expired.
        document.onvisibilitychange = () => {
            if (document.visibilityState === "visible") {
                this.startKeepSessionAlive();
            } else if (document.visibilityState === "hidden") {
                // This might not get triggered in Safari:
                // https://developer.mozilla.org/en-US/docs/Web/API/Document/onvisibilitychange
                this.stopKeepSessionAlive();
            }
        };

        // If the user is on the modeller page unauthenticated and
        // they login in a separate window, then the modeller page
        // will not immediately pick up the new authenticated state.
        // If the modeller page is not visible, then making it visible
        // will restart pinging the server (above), however if the
        // modeller page was already visible, we need instead to
        // restart pinging the server when the modeller page gets
        // the focus.
        window.onfocus = () => {
            this.startKeepSessionAlive();
        };
    }

    componentDidMount() {
        this.el = this.refs["ss-header"];
        this.$el = $(this.el);
        //console.log("this.el: ", this.el);
        this.bindEventHandlers();
    }

    bindEventHandlers() {
        //console.log("bindEventHandlers");
        this.$el.on("mouseover", (event) => {
            //console.log("Header: mouseover");
            //console.log("this.refs: ", this.refs);
            //N.B. although the following refers to side panel, we reuse this for the header
            if (!this.props.modeller) return; //Following only applies to the modeller page!
            if (this.props.modeller.sidePanelActivated) {
                //console.log("(side panel already activated)");
            } else {
                //console.log("dispatch sidePanelActivated");
                this.props.dispatch(sidePanelActivated());
            }
        });
    }

    componentDidUpdate() {
        let auth = this.props.auth;
        let previouslyValidSession =
            this.state.keepSessionAlive.confirmedValidSession;

        if (auth.hadResponse) {
            if (auth.isAuthenticated) {
                if (!previouslyValidSession) {
                    this.setState((state) => ({
                        ...state,
                        keepSessionAlive: {
                            ...state.keepSessionAlive,
                            confirmedValidSession: true,
                        },
                    }));
                }
            } else {
                // If the server responds with a 401 (user is not
                // authenticated) then we stop pinging the server
                // to prevent the console filling with errors.
                this.stopKeepSessionAlive();

                if (previouslyValidSession) {
                    this.setState((state) => ({
                        ...state,
                        keepSessionAlive: {
                            ...state.keepSessionAlive,
                            confirmedValidSession: false,
                        },
                    }));
                }

                // On pages other than the modeller we load the welcome
                // page as the user must be authenticated on those pages.
                // In all cases, if we had a previously valid session,
                // then the session must have expired, so we alert the
                // user.
                if (this.props.modeller) {
                    if (previouslyValidSession) {
                        alert(
                            "Your session has expired.\n" +
                                "\n" +
                                "You may continue to edit the model, but some actions may require you to login again."
                        );
                    }
                } else {
                    if (previouslyValidSession) {
                        alert("Your session has expired.");
                    }

                    loadWelcomePage();
                }
            }
        }
    }

    render() {
        let { auth, modeller, model } = this.props;
        let modelName = model
            ? model.name
            : modeller
            ? modeller.model.name
            : null;
        let modelId = model ? model.id : modeller ? modeller.model.id : null;

        return !(this.props.selection && this.props.selection.mute) &&
            !process.env.config.EMBEDDED ? (
            <div className="ss-header" ref="ss-header">
                <div className="branding">
                    <h3>
                        <Link
                            className="link"
                            href=""
                            text={<span>SPYDE<b>RISK</b></span>}
                            dispatch={this.props.dispatch}
                        />
                    </h3>
                </div>
                <div className="left-block">
                    <div className="model-name">
                        <h4>
                            {modelName ? modelName : " "}
                        </h4>
                    </div>
                <div className="left-menu">
                    {this.props.modeller ? (
                        <div className="header-dropdown">
                            <DropdownButton
                                title="Export"
                                open={this.state.exportDropdownOpen}
                                onToggle={(isOpen) => {
                                    this.setState({
                                        ...this.state,
                                        exportDropdownOpen: isOpen,
                                    });
                                }}
                                id={"export-options"}
                            >
                                <OverlayTrigger
                                    delayShow={Constants.TOOLTIP_DELAY}
                                    placement="right"
                                    overlay={
                                        <Tooltip
                                            id="tooltip"
                                            className={"tooltip-overlay"}
                                        >
                                            {
                                                "Export all assets, relations, threats, risks, etc"
                                            }
                                        </Tooltip>
                                    }
                                >
                                    <MenuItem
                                        key={2}
                                        eventKey={2}
                                        onClick={() => {
                                            this.props.dispatch(
                                                saveDownload(
                                                    "./models/" +
                                                        modelId +
                                                        "/export"
                                                )
                                            );
                                            this.setState({
                                                ...this.state,
                                                exportDropdownOpen: false,
                                            });
                                        }}
                                    >
                                        <span className="fa fa-floppy-o" />
                                        {" Export (full model)"}
                                    </MenuItem>
                                </OverlayTrigger>
                                <OverlayTrigger
                                    delayShow={Constants.TOOLTIP_DELAY}
                                    placement="right"
                                    overlay={
                                        <Tooltip
                                            id="tooltip"
                                            className={"tooltip-overlay"}
                                        >
                                            {
                                                "Export only user-defined assets, relations (no threats or risks)"
                                            }
                                        </Tooltip>
                                    }
                                >
                                    <MenuItem
                                        key={3}
                                        eventKey={3}
                                        onClick={() => {
                                            this.props.dispatch(
                                                saveDownload(
                                                    "./models/" +
                                                        modelId +
                                                        "/exportAsserted"
                                                )
                                            );
                                            this.setState({
                                                ...this.state,
                                                exportDropdownOpen: false,
                                            });
                                        }}
                                    >
                                        <span className="fa fa-file-o" />
                                        {" Export (asserted assets only)"}
                                    </MenuItem>
                                </OverlayTrigger>
                            </DropdownButton>
                        </div>
                    ) : null}

                    {this.props.modeller ? (
                        <div className="header-dropdown">
                            <DropdownButton
                                title="View"
                                open={this.state.viewSettingsOpen}
                                onToggle={(isOpen) => {
                                    this.setState({
                                        ...this.state,
                                        viewSettingsOpen: isOpen,
                                    });
                                }}
                                id={"view-settings"}
                            >
                                <OverlayTrigger
                                    delayShow={Constants.TOOLTIP_DELAY}
                                    placement="right"
                                    overlay={
                                        <Tooltip
                                            id="tooltip"
                                            className={"tooltip-overlay"}
                                        >
                                            {
                                                "Show/Hide the inferred relations between assets"
                                            }
                                        </Tooltip>
                                    }
                                >
                                    <MenuItem
                                        key={1}
                                        eventKey={1}
                                        onClick={() => {
                                            let currentState =
                                                this.props.modeller.view
                                                    .showInferredRelations;
                                            // update symbol for the future value of currentState
                                            if (currentState === true) {
                                                document.getElementById(
                                                    "showInferredRelation"
                                                ).className = "fa fa-square-o";
                                            } else {
                                                document.getElementById(
                                                    "showInferredRelation"
                                                ).className =
                                                    "fa fa-check-square-o";
                                            }
                                            this.props.dispatch(
                                                showInferredRelations(!currentState)
                                            );
                                            this.setState({
                                                ...this.state,
                                                showInferredRelations:
                                                    !currentState,
                                            });
                                        }}
                                    >
                                        <span
                                            className="fa fa-square-o"
                                            id="showInferredRelation"
                                        />
                                        {" Inferred Relations"}
                                    </MenuItem>
                                </OverlayTrigger>
                                <OverlayTrigger
                                    delayShow={Constants.TOOLTIP_DELAY}
                                    placement="right"
                                    overlay={
                                        <Tooltip
                                            id="tooltip"
                                            className={"tooltip-overlay"}
                                        >
                                            {
                                                "Show/Hide the hidden relations between assets"
                                            }
                                        </Tooltip>
                                    }
                                >
                                    <MenuItem
                                        key={2}
                                        eventKey={2}
                                        onClick={() => {
                                            let currentState =
                                                this.props.modeller.view
                                                    .showHiddenRelations;
                                            // update symbol for the future value of currentState
                                            if (currentState === true) {
                                                document.getElementById(
                                                    "showHiddenRelations"
                                                ).className = "fa fa-square-o";
                                            } else {
                                                document.getElementById(
                                                    "showHiddenRelations"
                                                ).className =
                                                    "fa fa-check-square-o";
                                            }
                                            this.props.dispatch(
                                                showHiddenRelations(!currentState)
                                            );
                                            this.setState({
                                                ...this.state,
                                                showHiddenRelations: !currentState,
                                            });
                                        }}
                                    >
                                        <span
                                            className="fa fa-square-o"
                                            id="showHiddenRelations"
                                        />
                                        {" Hidden Relations"}
                                    </MenuItem>
                                </OverlayTrigger>
                                <OverlayTrigger
                                    delayShow={Constants.TOOLTIP_DELAY}
                                    placement="right"
                                    overlay={
                                        <Tooltip
                                            id="tooltip"
                                            className={"tooltip-overlay"}
                                        >
                                            {
                                                "Re-centre the canvas going back to the original position"
                                            }
                                        </Tooltip>
                                    }
                                >
                                    <MenuItem
                                        key={3}
                                        eventKey={3}
                                        onClick={() => {
                                            let currentState =
                                                this.props.modeller.view
                                                    .reCentreCanvas;
                                            this.props.dispatch(
                                                reCentreCanvas(!currentState)
                                            );
                                        }}
                                    >
                                        <span className="fa fa-compass" />
                                        {" Centre Canvas"}
                                    </MenuItem>
                                </OverlayTrigger>
                                <OverlayTrigger
                                    delayShow={Constants.TOOLTIP_DELAY}
                                    placement="right"
                                    overlay={
                                        <Tooltip
                                            id="tooltip"
                                            className={"tooltip-overlay"}
                                        >
                                            {
                                                "Re-centre the model moving all assets to the centre of the Canvas"
                                            }
                                        </Tooltip>
                                    }
                                >
                                    <MenuItem
                                        key={4}
                                        eventKey={4}
                                        onClick={() => {
                                            let currentState =
                                                this.props.modeller.view
                                                    .reCentreModel;
                                            this.props.dispatch(
                                                reCentreModel(!currentState)
                                            );
                                        }}
                                    >
                                        <span className="fa fa-arrows" />
                                        {" Centre Model"}
                                    </MenuItem>
                                </OverlayTrigger>
                            </DropdownButton>
                        </div>
                    ) : null}

                        <div className="header-dropdown">
                            <DropdownButton title="Help" id="help-menu">
                                <MenuItem
                                    key={0}
                                    eventKey={0}
                                    onClick={(e) =>
                                        openDocumentation(e, "Reference%20Guide/")
                                    }
                                >
                                    Reference Guide
                                </MenuItem>
                                <MenuItem
                                    key={10}
                                    eventKey={10}
                                    onClick={(e) =>
                                        openApiDocs(e)
                                    }
                                >
                                    Spyderisk REST API
                                </MenuItem>
                                <MenuItem divider />
                                <h4 style={{ color: "black", paddingLeft: "20px" }}>
                                    Tutorials
                                </h4>
                                <MenuItem
                                    key={1}
                                    eventKey={1}
                                    onClick={(e) =>
                                        openDocumentation(e, "Tutorials/1%20Overview/")
                                    }
                                >
                                    1. Overview
                                </MenuItem>
                                <MenuItem
                                    key={2}
                                    eventKey={2}
                                    onClick={(e) =>
                                        openDocumentation(e, "Tutorials/2%20Getting%20Started/")
                                    }
                                >
                                    2. Getting Started with SSM
                                </MenuItem>
                                <MenuItem
                                    key={3}
                                    eventKey={3}
                                    onClick={(e) =>
                                        openDocumentation(e, "Tutorials/3%20Risk%20Identification%20pt1%20-%20Modelling%20your%20System/")
                                    }
                                >
                                    3. Risk Identification (1) - Modelling your System
                                </MenuItem>
                                <MenuItem
                                    key={4}
                                    eventKey={4}
                                    onClick={(e) =>
                                        openDocumentation(e, "Tutorials/4%20Risk%20Identification%20pt2%20-%20Validating%20your%20Model/")
                                    }
                                >
                                    4. Risk Identification (2) - Validating your Model
                                </MenuItem>
                                <MenuItem
                                    key={5}
                                    eventKey={5}
                                    onClick={(e) =>
                                        openDocumentation(e, "Tutorials/5%20Risk%20Analysis%20-%20Impact%20and%20Controls/")
                                    }
                                >
                                    5. Risk Analysis (1)
                                </MenuItem>
                                <MenuItem
                                    key={6}
                                    eventKey={6}
                                    onClick={(e) =>
                                        openDocumentation(e, "Tutorials/6%20Risk%20Analysis%20and%20Evaluation/")
                                    }
                                >
                                    6. Risk Analysis (2) and Risk Evaluation
                                </MenuItem>
                                <MenuItem
                                    key={7}
                                    eventKey={7}
                                    onClick={(e) =>
                                        openDocumentation(e, "Tutorials/7%20Risk%20Treatment/")
                                    }
                                >
                                    7. Risk Treatment
                                </MenuItem>
                                <MenuItem
                                    key={8}
                                    eventKey={8}
                                    onClick={(e) =>
                                        openDocumentation(e, "Tutorials/8%20Finishing%20your%20Session/")
                                    }
                                >
                                    8. Finishing your Session
                                </MenuItem>
                            </DropdownButton>
                        </div>
                    </div>
                </div>
            {!process.env.config.BRAND_ONLY && (
                <div className="right">
                    <Link
                        className="link"
                        href=""
                        text="Home"
                        dispatch={this.props.dispatch}
                    />
                    <Link
                        className="link"
                        href="/dashboard"
                        text="Dashboard"
                        dispatch={this.props.dispatch}
                    />
                    <div id="app-extension-container" />
                    <DropdownButton
                        title={
                            auth.user === "" || auth.user === null ? (
                                "Account"
                            ) : auth.user.username !== "test" ? (
                                <span>
                                    {auth.user.firstName
                                        ? auth.user.firstName
                                        : auth.user.username}
                                </span>
                            ) : (
                                <span className="text-danger">
                                    {auth.user.username}
                                    &nbsp;(DEBUG)
                                </span>
                            )
                        }
                        id="dropdown"
                        pullRight
                    >
                        {auth.user === "" ||
                        auth.user === null ||
                        auth.user.role !== 1
                            ? []
                            : [
                                <MenuItem key={4.1} header>
                                    Administration
                                </MenuItem>,
                                <LoadPageMenuItem
                                    key={4.2}
                                    href="/domain-manager"
                                    text="Knowledgebase Manager"
                                    dispatch={this.props.dispatch}
                                    restricted={true}
                                />,
                                <LoadPageMenuItem
                                    key={4.3}
                                    href="/admin"
                                    text="Admin Panel"
                                    dispatch={this.props.dispatch}
                                    restricted={true}
                                />,
                            ]}
                            {auth.user === "" || auth.user === null
                                ?
                                <MenuItem
                                    key={3}
                                    href={
                                        process.env.config.END_POINT +
                                        "/dashboard"
                                    }
                                >
                                    Login
                                </MenuItem>
                                :
                                <Fragment>
                                    <MenuItem
                                        key={3.1}
                                        header
                                    >
                                        User -{" "}
                                        {auth.user.firstName
                                            ? auth.user.firstName
                                            : auth.user.username}
                                    </MenuItem>
                                    <MenuItem
                                        key={3.2}
                                        href={"/auth/realms/ssm-realm/account/"}
                                    >
                                        Manage Account
                                    </MenuItem>
                                    <MenuItem
                                        key={3.3}
                                        href={process.env.config.END_POINT +
                                            "/logout"}
                                    >
                                        Sign Out
                                    </MenuItem>
                                </Fragment>
                            }
                    </DropdownButton>
                </div>
            )}
            {process.env.BRAND_ONLY && (
                <div className="right">
                    <div id="app-extension-container" />
                </div>
            )}
            </div>
        ) : null;
    }

    startKeepSessionAlive() {
        if (this.state.keepSessionAlive.timerId === null) {
            // Clear any previous response.
            this.props.dispatch({
                type: actions.RESET_GET_USER,
            });

            this.props.dispatch(getUser());

            // Ping the server every 30 seconds to keep the session alive.
            let timerId = setInterval(
                () => this.props.dispatch(getUser()),
                30000
            );

            this.setState((state) => ({
                ...state,
                keepSessionAlive: {
                    ...state.keepSessionAlive,
                    timerId: timerId,
                },
            }));

            //console.log("Started keep session alive timer: " + timerId);
        }
    }

    stopKeepSessionAlive() {
        if (this.state.keepSessionAlive.timerId !== null) {
            let timerId = this.state.keepSessionAlive.timerId;

            clearInterval(timerId);

            this.setState((state) => ({
                ...state,
                keepSessionAlive: {
                    ...state.keepSessionAlive,
                    timerId: null,
                },
            }));

            //console.log("Stopped keep session alive timer: " + timerId);
        }
    }
}

let getUserRole = (key) => {
    switch (key) {
        case 1:
            return "Administrator";
        case 2:
            return "Standard User";
    }
};

Header.propTypes = {
    modelName: PropTypes.string,
    auth: PropTypes.object,
    sidePanelActivated: PropTypes.bool,
    dispatch: PropTypes.func,
};

let mapStateToProps = function (state) {
    return {
        auth: state.auth,
        modeller: state.modeller,
        model: state.model,
        selection: state.selection,
    };
};

export default connect(mapStateToProps)(Header);
