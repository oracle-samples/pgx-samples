# PGQL on RDBMS Samples

Oracle Graph has the ability to run PGQL queries using the in memory server (PGX) or in database (RDBMS). This directory contains sample queries for using PGQL on RDBMS, using the Bank Graph Dataset. 

## Prerequisites
1. SQL Developer 23.1 is installed
2. A graph enabled user exists in the connected database

## Load Bank Graph Dataset
1. Open SQL Developer
2. Create tables from Bank Graph Dataset, using CreateBankGraphDataset.sql
3. Load BANK_ACCOUNTS.csv into BANK_ACCOUNTS table
4. Load BANK_TRANSFERS.csv into BANK_TRANSFERS table

## Run Examples on a PGQL Worksheet
1. Open PGQL Worksheet
2. Copy Create Proprty Graph statement from bank-graph-pgql and run the statement
3. Copy and paste queries from bank-graph-pgql to PGQL worksheet
4. Highlight individual queries and run