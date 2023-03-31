import React, {Component} from "react";
import "./Page.scss";
import Header from "../header/Header";
import PropTypes from "prop-types";

class Page extends Component {

    render() {
        return (
            <div className="ss-page">
                {!process.env.EMBEDDED && <Header/>}
                <div className="ss-page-content">
                    {this.props.children}
                </div>
                {/*
                <Footer/>
                */}
            </div>
        );
    }

}

Page.contextTypes = {
    store: PropTypes.object.isRequired,
};

export default Page;
