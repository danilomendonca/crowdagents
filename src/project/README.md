
In this project, mobile applications have an agent responsible for different autonomous tasks. 
Task assignment must consider the physical and social context of devices.  


NodeAgent

- check for its own physical and social context
-- retrieve data from battery, sensors, etc
- calculates its fitness value 
- advertises its fitness value
- compare its fitness value with the others
--  if bigger, play the role
-- otherwise, do nothing


Q: What about the fitness function knowledge?

Now, I can assume the node agents will have the FF for different sensing roles hard coded. 

Q: What about k?

Now, I can assume the value of k is passed as a parameter.

Q: What about n?

Now, I can assume the value of n is passed as a parameter.

