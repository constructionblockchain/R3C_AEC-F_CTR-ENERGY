P2P Energy Marketplace for Neighborhood 

To Run: 
NodeDriver.kt will start a network of 5 neighbours, a utility company, and a bank.
Each 30 minutes, for each neighbour, a reading will be simulated between -50kWh and + 50kWh.

One of the neighbours will collect all readings, run the netting algorithm and collect the payments.

To visualise the status, go to: ``http://localhost:10007/web/neighbour/`` ( or ``http://localhost:10011/web/neighbour/``, etc)
