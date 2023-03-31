import React, { Component } from "react";
import { Col, Row } from "react-bootstrap";
import RecentCard from "../RecentCard/RecentCard";
import './RecentList.scss';


class RecentList extends Component {
    constructor(props) {
        super(props);
        this.state = {
           open: true
        };
    }

    render() {
        let {models} = this.props;
        return (
            <div>
                <h3 className={"recent-list-title"}>Recent models</h3>
                <Row className={"row-list"}>
                    {
                        models.length != 0 ?
                            models.sort((a, b) => {
                                return this.compare(a,b);
                            }).slice(0,4).map((card,i) =>
                                <RecentCard
                                    className={"cards"}
                                    key={i}
                                    model={card}
                                    dispatch={this.props.dispatch}
                                    models={models}/>
                            )
                        :
                            <Col xs={12} md={12} lg={12}>
                                <p>You don't have any models. Use the Create or Import buttons.</p>
                            </Col>
                    }
                </Row>
            </div>
        );
    }

    compare( a, b ) {
        if ( a.modified < b.modified ){
            return 1;
        }
        if ( a.modified > b.modified ){
            return -1;
        }
        return 0;
    }

    showHide () {
        this.setState({
            open: !this.state.open
        })
    }

}

export default RecentList;
