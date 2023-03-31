import React, {Component} from "react";
import PropTypes from "prop-types";
import {MenuItem} from "react-bootstrap";
import {loadPage} from "../../actions/api";

class LoadPageMenuItem extends Component {
    render() {
        return <MenuItem className={this.props.className}
                         href={process.env.config.END_POINT + this.props.href}
                         onClick={(event) => this.props.dispatch(
                             loadPage(this.props.href, this.props.restricted, event)
                         )}>
            {this.props.text}
        </MenuItem>
    }
}

LoadPageMenuItem.propTypes = {
    className: PropTypes.string,
    text: PropTypes.string,
    href: PropTypes.string,
    restricted: PropTypes.bool,
    dispatch: PropTypes.func,
};

LoadPageMenuItem.defaultProps = {
    className: "",
    restricted: false,
};

export default LoadPageMenuItem;
