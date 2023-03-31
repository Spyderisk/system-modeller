import React, { Component } from "react";
import BootstrapTable from "react-bootstrap-table-next";
import paginationFactory from 'react-bootstrap-table2-paginator';
import filterFactory, { textFilter } from 'react-bootstrap-table2-filter';
import Link from "../../../common/components/link/Link";

class UserTable extends Component {

    constructor(props) {
        super(props);
    }

    render() {
        const columns = [{
            dataField: 'id',
            text: 'ID',
            editable: false,
            sort: true,
            filter: textFilter()
        }, {
            dataField: 'username',
            text: 'Username',
            editable: false,
            sort: true,
            filter: textFilter()
        }, {
            dataField: 'email',
            text: 'Email',
            editable: false,
            sort: true,
            filter: textFilter()
        }, {
            dataField: 'firstName',
            text: 'First Name',
            editable: false,
            sort: true,
            filter: textFilter()
        }, {
            dataField: 'lastName',
            text: 'Last Name',
            editable: false,
            filter: textFilter(),
            sort: true
        }, {
            dataField: 'role',
            text: 'Role',
            editable: false,
            sort: true,
            formatter: (cell, row) => {
                return (row.role === 1 ? "Admin" : "User");
            }
        }, {
            dataField: 'enabled',
            text: 'Is Enabled?',
            editable: false,
            sort: true,
            formatter: (cell, row) => {
                return (row.enabled);
            }
        }, {
            dataField: 'modelsCount',
            text: 'Models',
            editable: false,
            sort: true,
            formatter: (cell, row) => {
                return <span>
                    {row.modelsCount.toString()} <Link href={"/dashboard/" + row.id} text={"(view)"} dispatch={this.props.dispatch} restricted={true} />
                </span>
            }
        }];

        return (
            <div>
                <BootstrapTable keyField='id' data={this.props.users} columns={columns} pagination={ paginationFactory() } filter={ filterFactory() } filterPosition='top' >
                    noDataIndication="Table is Empty" striped hover condensed>
                </BootstrapTable>
            </div>
        )
    }
}

export default UserTable;
