import React, { Component } from "react";
import PropTypes from 'prop-types';
import { Pagination } from "react-bootstrap";

/**
    The PagedPanel takes an array of React or HTML elements that should be displayed
    and a few options of how they should be split onto different pages. All logic for
    managing pages and cotnent is taken care of internally
*/
class PagedPanel extends Component {

    constructor(props) {
        //console.log("constructor:", props.context);
        super(props);
        this.state = {
            activePage: 1
        };
        this.generateButtons = this.generateButtons.bind(this);
    }

    componentWillReceiveProps(nextProps, nextContext) {
        //console.log("this.props:", this.props);
        //console.log("nextProps:", nextProps);
        let reset_page = false;
        
        if (this.props.context !== nextProps.context) {
            //console.log("context changed (" + this.props.context + " -> " + nextProps.context + ")");
            reset_page = true;
        }
        else if (nextProps.panelData.length !== this.props.panelData.length) {
            //console.log("panelData length changed");
            reset_page = true;
        }
        
        if (reset_page) {
            //console.log("resetting activePage to 1");
            this.setState({
                ...this.state,
                activePage: 1
            });
        }
    }

    render() {
        //console.log("context:", this.props.context);

        let panelData = this.props.panelData;
        let pageSize = this.props.pageSize;

        if (panelData.length === 0){
            return (<p>{this.props.noDataMessage}</p>);
        }
        let maxPage = Math.ceil(panelData.length/pageSize);

        //Calculate the range of elements that need to be displayed
        //console.log("this.state.activePage:", this.state.activePage);
        let firstIndex = (this.state.activePage - 1) * pageSize;
        let lastIndex = (this.state.activePage * pageSize); //must be one more than the required last index!
        let dataToDisplay = panelData.slice(firstIndex, lastIndex);
        //console.log("firstIndex:", firstIndex);
        //console.log("lastIndex:", lastIndex);
        //console.log("dataToDisplay:", dataToDisplay);

        return (
            <div>
            {Object.values(dataToDisplay).map((data) => {
                return data;
            })}
            { maxPage > 1 ? <Pagination style={{
                display: "block",
                marginLeft: "auto",
                marginRight: "auto",
                width: "200px"
            }}>{this.generateButtons(maxPage)}</Pagination> : "" }
            </div>
        );
    }

    //Buttons for navigating the different pages in the pagination
    generateButtons(maxPage) {
        let items = [];
        let activePage = this.state.activePage;
        items.push(<Pagination.First key="0" disabled={activePage==1} onClick={() => this.setState({
              ...this.state,
              activePage: 1
            })}/>);

        let prev = activePage<=1 ? activePage : activePage-1;
        items.push(<Pagination.Prev key="1" disabled={activePage==1} onClick={() => this.setState({
            ...this.state,
            activePage: prev
            })}/>);

        items.push(<Pagination.Item key="2" active>{activePage + " / " + maxPage}</Pagination.Item>);

        let next = activePage>=maxPage ? activePage : activePage+1;
        items.push(<Pagination.Next key="3" disabled={activePage==maxPage} onClick={() => this.setState({
            ...this.state,
            activePage: next
            })}/>);
        items.push(<Pagination.Last key="4" disabled={activePage==maxPage} onClick={() => this.setState({
            ...this.state,
            activePage: maxPage
            })}/>);

        return items;
    }

}

//These are the default settings, that should be overwritten by modifying the props
PagedPanel.defaultProps = {
    panelData: [],
    pageSize: 15,
    context: "",
    noDataMessage: "Nothing to show"
};

PagedPanel.propTypes = {
    panelData: PropTypes.array.isRequired,
    pageSize: PropTypes.number,
    context: PropTypes.string,
    noDataMessage: PropTypes.string
};

export
default
PagedPanel;
