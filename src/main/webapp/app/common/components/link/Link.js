import React, {Component} from "react";
import PropTypes from "prop-types";
import {loadPage} from "../../actions/api";
import "./Link.scss";

class Link extends Component {
    render() {
        return <a className={this.props.className}
                  href={process.env.config.END_POINT + this.props.href}
                  onClick={(event) => this.props.dispatch(
                      loadPage(this.props.href, this.props.restricted, event)
                  )}>
            {this.props.icon}
            {this.props.text}
        </a>;
    }
}

Link.propTypes = {
    icon: PropTypes.object,
    className: PropTypes.string,
    text: PropTypes.string,
    href: PropTypes.string,
    restricted: PropTypes.bool,
    dispatch: PropTypes.func,
};

Link.defaultProps = {
    icon: <i/>,
    className: "",
    text: "",
    restricted: false,
};

export default Link;
