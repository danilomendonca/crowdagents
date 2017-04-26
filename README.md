#crowdagents
======

##Intelligent Multiagent Systems Final Project


###Summary

In this project, an autonomous agent is responsible for different application behaviors (tasks). Task assignment must consider the physical and social context of devices, meaning their current capabilities and state. The assignment follows a Sequential Single Item allocation market based method. Each agent must check its own cost (in this case, fitness) to perform a task. Once calculated, agents advertise their fitness value (FV) to all other agents, who will do the same. At the end of that cycle, agents share a common knowledge about their fitness to perform a given task. Based on the total number of instances k that must exist, the first k agents with the highest FV will perform the task.
  
###Description

NodeAgent behavior:

- check for its own physical and social context
-- retrieve data from battery, sensors, etc
- calculates its fitness value 
- advertises its fitness value
- compare its fitness value with the others
--  if bigger, play the role; otherwise, do nothing

###Open Questions

Q: What about the fitness function knowledge?

Now, I can assume the node agents will have the FF for different sensing roles hard coded. 

Q: What about k?

Now, I can assume the value of k is passed as a parameter.

Q: What about n?

Now, I can assume the value of n is passed as a parameter.

