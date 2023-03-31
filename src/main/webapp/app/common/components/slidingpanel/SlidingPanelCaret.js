import React, {Component} from "react";
import PropTypes from 'prop-types';
import './SlidingPanelCaret.scss';

class SlidingPanelCaret extends Component {

    constructor(props) {
        super(props);

        this.handleMouseClick = this.handleMouseClick.bind(this);
        this.state = {
            timeout: false
        }
    }

    handleMouseClick(e) {
        this.props.resizePanelWidth(this.props.isOutwards ? -2 : -1);
    }

    render() {
        return (<div
            className={"ssm-sliding-panel-caret " + (this.props.isLeft ? "left" : "right")}
            onClick={this.handleMouseClick}>
            {
                this.props.isLeft ?
                    this.props.isOutwards ?
                        <i className={"fa fa-2x fa-angle-double-left"}/>
                        :
                        <i className={"fa fa-2x fa-angle-double-right"}/>
                    :
                    this.props.isOutwards ?
                        <i className={"fa fa-2x fa-angle-double-right"}/>
                        :
                        <i className={"fa fa-2x fa-angle-double-left"}/>
            }

        </div>);
    }

}

SlidingPanelCaret.defaultProps = {
    isLeft: true,
    isOutwards: true
};

SlidingPanelCaret.propTypes = {
    isLeft: PropTypes.bool.isRequired,
    isOutwards: PropTypes.bool.isRequired,
    resizePanelWidth: PropTypes.func
};

export default SlidingPanelCaret;