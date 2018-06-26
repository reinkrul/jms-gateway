# Supportability
JMS Gateway currently supports the following databases:
* PostgreSQL

And the following JMS brokers are supported:
* ActiveMQ Artemis (tested with 2.6.2)

# Important notes
* Make sure your messsage UUIDs are in lowercase. Otherwise the JDBC consumer won't work at all. 

# TODO
Write test cases for the following situations:
* jms_message table does not exist