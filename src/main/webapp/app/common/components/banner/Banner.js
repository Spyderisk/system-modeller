import React, {Component} from "react";
import PropTypes from "prop-types";

class Banner extends Component {

    render() {

        return (
            <div className="banner">
                <div className="title">
                    <h1>{this.props.title}</h1>
                </div>
                <div className="options">
                    {this.props.options.map(node => node)}
                </div>
            </div>
        );
    }
}

Banner.defaultProps = {
    title: "",
    options: []
};

Banner.propTypes = {
    title: PropTypes.string.isRequired,
    options: PropTypes.array
};

export default Banner;