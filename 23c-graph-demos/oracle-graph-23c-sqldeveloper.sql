-- Drop BANK_GRAPH and tables if they exist
DROP PROPERTY GRAPH BANK_GRAPH;
DROP TABLE BANK_TRANSFERS;
DROP TABLE BANK_ACCOUNTS;

-- create BANK ACCOUNTS table
CREATE TABLE BANK_ACCOUNTS (
    ID              NUMBER,
    NAME            VARCHAR(400),
    BALANCE         NUMBER(20,2)
);

-- create BANK_TRANSFERS table
CREATE TABLE BANK_TRANSFERS (
    TXN_ID          NUMBER,
    SRC_ACCT_ID     NUMBER,
    DST_ACCT_ID     NUMBER,
    DESCRIPTION     VARCHAR(400),
    AMOUNT          NUMBER
);

-- Add constraints
ALTER TABLE BANK_ACCOUNTS ADD PRIMARY KEY (ID);
ALTER TABLE BANK_TRANSFERS ADD PRIMARY KEY (TXN_ID);
ALTER TABLE BANK_TRANSFERS MODIFY SRC_ACCT_ID REFERENCES BANK_ACCOUNTS (ID);
ALTER TABLE BANK_TRANSFERS MODIFY DST_ACCT_ID REFERENCES BANK_ACCOUNTS (ID);

-- Optionally verify constraints
SELECT * FROM USER_CONS_COLUMNS WHERE table_name IN ('BANK_ACCOUNTS', 'BANK_TRANSFERS');

-- At this point, you should load the BANK_ACCOUNTS.csv and BANK_TRANSFERS.csv files into the BANK_ACCOUNTS and BANK_TRANSACTIONS tables respectively
-- Create a property graph view on bank_accounts and bank_transfers
CREATE PROPERTY GRAPH BANK_GRAPH 
    VERTEX TABLES (
        BANK_ACCOUNTS
        KEY (ID)
        PROPERTIES (ID, Name, Balance) 
    )
    EDGE TABLES (
        BANK_TRANSFERS 
        KEY (TXN_ID) 
        SOURCE KEY (src_acct_id) REFERENCES BANK_ACCOUNTS(ID)
        DESTINATION KEY (dst_acct_id) REFERENCES BANK_ACCOUNTS(ID)
        PROPERTIES (src_acct_id, dst_acct_id, amount)
    );

-- This query shows the graphs available for the current user    
SELECT * FROM user_property_graphs;

-- This query shows the DDL for the BANK_TRANSFERS graph 
SELECT dbms_metadata.get_ddl('PROPERTY_GRAPH', 'BANK_GRAPH') from dual;

-- This query shows the elements for the BANK_TRANSFERS graph
SELECT * FROM user_pg_elements WHERE graph_name='BANK_GRAPH';

-- This query shoes the labels and properties available in the BANK_TRANSFERS graph
SELECT * FROM user_pg_label_properties WHERE graph_name='BANK_GRAPH';

-- Find the top 10 accounts by incoming transfers 
SELECT acct_id, COUNT(1) AS Num_Transfers 
    FROM graph_table ( BANK_GRAPH 
        MATCH (src) - [IS BANK_TRANSFERS] -> (dst) 
        COLUMNS ( dst.id AS acct_id )
    ) GROUP BY acct_id ORDER BY Num_Transfers DESC FETCH FIRST 10 ROWS ONLY;
    
-- Find the top 10 accounts in the middle of a 2-hop chain of transfers
SELECT acct_id, COUNT(1) AS Num_In_Middle 
    FROM graph_table ( BANK_GRAPH 
        MATCH (src) - [IS BANK_TRANSFERS] -> (via) - [IS BANK_TRANSFERS] -> (dst) 
        COLUMNS ( via.id AS acct_id )
    ) GROUP BY acct_id ORDER BY Num_In_Middle DESC FETCH FIRST 10 ROWS ONLY;
    
-- List accounts that received a transfer from account 387 in 1, 2, or 3 hops
SELECT account_id1, account_id2 
    FROM graph_table(BANK_GRAPH
        MATCH (v1)-[IS BANK_TRANSFERS]->{1,3}(v2) 
        WHERE v1.id = 387 
        COLUMNS (v1.id AS account_id1, v2.id AS account_id2)
    );
    
-- Check if there are any 3-hop (triangles) transfers that start and end at the same account
SELECT acct_id, COUNT(1) AS Num_Triangles 
    FROM graph_table (BANK_GRAPH 
        MATCH (src) - []->{3} (src) 
        COLUMNS (src.id AS acct_id) 
    ) GROUP BY acct_id ORDER BY Num_Triangles DESC;
    
-- Check if there are any 4-hop transfers that start and end at the same account
SELECT acct_id, COUNT(1) AS Num_4hop_Chains 
    FROM graph_table (BANK_GRAPH 
        MATCH (src) - []->{4} (src) 
        COLUMNS (src.id AS acct_id) 
    ) GROUP BY acct_id ORDER BY Num_4hop_Chains DESC;

-- Check if there are any 5-hop transfers that start and end at the same account
SELECT acct_id, COUNT(1) AS Num_5hop_Chains 
    FROM graph_table (BANK_GRAPH 
        MATCH (src) - []->{5} (src) 
        COLUMNS (src.id AS acct_id) 
    ) GROUP BY acct_id ORDER BY Num_5hop_Chains DESC;

-- List some (any 10) accounts which had a 3 to 5 hop circular payment chain 
SELECT DISTINCT(account_id) 
    FROM GRAPH_TABLE(BANK_GRAPH
       MATCH (v1)-[IS BANK_TRANSFERS]->{3,5}(v1)
        COLUMNS (v1.id AS account_id)  
    ) FETCH FIRST 10 ROWS ONLY;

-- Query accounts by number of 3 to 5 hops cycles in descending order. Show top 10. 
SELECT DISTINCT(account_id), COUNT(1) AS Num_Cycles 
    FROM graph_table(BANK_GRAPH
        MATCH (v1)-[IS BANK_TRANSFERS]->{3, 5}(v1) 
        COLUMNS (v1.id AS account_id) 
    ) GROUP BY account_id ORDER BY Num_Cycles DESC FETCH FIRST 10 ROWS ONLY;























