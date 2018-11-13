P2P Energy Marketplace for Neighborhood 

To Run: 
NodeDriver.kt will start a network of 5 neighbours, a utility company, and a bank.
The bank will issue £100 to every neighbour and the utility company.

Then in a loop for each neighbour a reading will be simulated between -50kWh and + 50kWh.

Each iteration, one of the neighbours will collect all readings, run the netting algorithm and collect the payments.

To visualise the status, each neighbour opens a web page. ( the URLs are listed in the program output)

e.g go to: ``http://localhost:10007/web/neighbour/`` ( or ``http://localhost:10011/web/neighbour/``, etc)
