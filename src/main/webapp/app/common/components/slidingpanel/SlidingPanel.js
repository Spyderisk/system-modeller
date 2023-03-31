import React, {Component} from "react";
import PropTypes from 'prop-types';
import SlidingPanelHandle from "./SlidingPanelHandle";
import SlidingPanelCaret from "./SlidingPanelCaret";
import './SlidingPanel.scss';
import {updateSidePanelWidths, sidePanelActivated} from '../../../modeller/actions/ModellerActions.js'
import {connect} from "react-redux";

// This class is used for the left and right panels that slide in and out
class SlidingPanel extends Component {

    constructor(props) {
        super(props);

        this.propsUpdater = this.propsUpdater.bind(this);
        this.bindEventHandlers = this.bindEventHandlers.bind(this);

        //Top is only needed here in the case that the order of the ss-header render comes before the render of the
        //SlidingPanel which causes overlapping UI elements until the next time the sliding panel is rerendered
        this.state = {
            width: this.props.width,
            previousWidth: undefined,
            top: this.props.top
        };

        this.handleResizePanelWidth = this.handleResizePanelWidth.bind(this);
    }

    propsUpdater(size) {
        let newSidePanelWidths = {};
        if (this.props.isLeft) newSidePanelWidths.left = size;
        else newSidePanelWidths.right = size;
        this.props.dispatch(updateSidePanelWidths(newSidePanelWidths));
    }

    handleResizePanelWidth(size) {
        if (size > ($(document).outerWidth() * 2) / 3) {
            return;
        }
        if (size <= -2) {
            size = 0;
        } else if (size === -1) {
            size=this.state.previousWidth;
        }

        let previousWidth = this.state.width;
        this.setState({...this.state, width: size, previousWidth: previousWidth});

        let changedSize = Math.abs(size - previousWidth);
        if (changedSize > 100) {
            this.propsUpdater(size);
        } else if (changedSize > 10) {
            setTimeout(this.propsUpdater, 10, size);
        } else if (changedSize > 2) {
            setTimeout(this.propsUpdater, 4, size)
        }
    }

    componentDidMount() {
        this.el = this.refs["sliding-panel"];
        this.$el = $(this.el);
        //console.log("this.el: ", this.el);
        this.bindEventHandlers();
    }

    bindEventHandlers() {
        //console.log("bindEventHandlers");
        this.$el.on("mouseover", (event) => {
            //console.log("SlidingPanel: mouseover");
            //console.log("this.refs: ", this.refs);
            if (this.props.sidePanelActivated) {
                //console.log("(side panel already activated)");
            }
            else {
                //console.log("dispatch sidePanelActivated");
                this.props.dispatch(sidePanelActivated());
            }
        });
    }

    render() {
        let style = {
            'width': this.state.width + 'px',
            'top': this.state.top==null?'0':this.state.top
        };
        let panelStyle={};
        if(this.state.width === 0){
            panelStyle['display']='none';
        }
        if ($('.ss-header').length) {
           style["top"] = "75px";  // TODO: put this into a constant (it is used to push the panel under the top header bar if header exists)
        }
        return (
            <div style={style} ref="sliding-panel"  
                 className={"ssm-sliding-panel-container " + (this.props.isLeft ? "left" : "right")}>
                <SlidingPanelCaret isLeft={this.props.isLeft} isOutwards={this.state.width !== 0}
                                   resizePanelWidth={this.handleResizePanelWidth}/>
                {this.props.isResizable &&
                <SlidingPanelHandle {...this.props} resizePanelWidth={this.handleResizePanelWidth}
                                    isVisible={this.state.width === 0}/>}
                <div style={panelStyle} className="ssm-sliding-panel">
                    <div className="ssm-sliding-panel-title">
                        {this.props.title !== undefined ? this.props.title : null}
                    </div>
                    <div className={"ssm-sliding-panel-content " +
                            (this.props.clazzName!==undefined ? this.props.clazzName : "")}>
                        {this.props.children}
                    </div>
                </div>
            </div>
        );
    }

}

SlidingPanel.defaultProps = {
    isLeft: true,
    isResizable: false
};

SlidingPanel.propTypes = {
    isLeft: PropTypes.bool.isRequired,
    isResizable: PropTypes.bool,
    sidePanelActivated: PropTypes.bool,
    title: PropTypes.node,
    dispatch: PropTypes.func,
};

let mapStateToProps = function (state) {
    return {
        auth: state.auth,
    }
};

export default connect(mapStateToProps)(SlidingPanel);
