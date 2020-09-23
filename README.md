# An Effective Fleet Management Strategy for Collaborative Spatio-Temporal Searching (GIS Cup)

## Brief description
We first use the Uber H3 library (https://github.com/uber/h3-java) to divide the space into several hexagon regions. Then we employ a deep convolutional network model to predict future pickup and dropoff event number in each region. Given the predicted values, we calculate the region weight for each region and guide idle agents to nearby location in their k neighbor layers according to the weight. To get a more accurate travel time estimate, we use a multilayer perceptron (MLP) to predict the dynamic travel speeds of agents, which is important for assignment strategy. Furthermore, in order to promote collaboration between agents, we collect the real-time information of all idle agents in every time interval and reschedule these agents together. Specifically, we model the fleet management as a Minimum Cost Flow problem and solve it with Google OR-Tools (https://developers.google.com/optimization). Finally, we replace the nearest greedy order dispatch straetgy with a serial dispatch strategy, i.e. resources can be assigned to agents in service.

## The main ideas of our solution can be formulated as follows:
1. We construct a prediction model based on Graph Convolutional Network (GCN) for spatiotemporal resource prediction.
2. We predict the dynamic travel speeds of agents to get accurate travel time estimation.
3. To avoid herding effect, each agent is guided to a random location according to the region weight and we adopt a reschedule strategy to promote collaboration.
4. To reduce the passengers' waiting time, we replace the nearest greedy order dispatch straetgy with a serial dispatch strategy.


## Prerequisites
Java 8 or up
For Windows 10 64-bit (x86_64), Microsoft Visual C++ Redistributable for Visual Studio 2019 (https://visualstudio.microsoft.com/zh-hans/downloads/?q=Visual+C%2B%2B+Redistributable+for+Visual+Studio) must be installed, since OR-Tools library for Java is a wrapper for the C++ native library.
For detailed prequisites to install OP-Tools, please refer to https://developers.google.com/optimization/install.


## How to compile and run

- For Windows 10 64-bit (x86_64)

Copy the jniortools.dll from lib folder into C:\Windows\System32.
cd into the lib directory.
Run "mvn install:install-file -Dfile=com.google.ortools.jar -DgroupId=com.google.ortools -DartifactId=com-google-ortools -Dversion=7.8.7959 -Dpackaging=jar" and "mvn install:install-file -Dfile=protobuf.jar -DgroupId=com.google.ortools -DartifactId=protobuf -Dversion=7.8.7959 -Dpackaging=jar"
Run mvn exec:java -Dexec.mainClass="Main" to perform simulation.

   If you have some preblems in building the project in intellij, please refer to: https://stackoverflow.com/questions/58819070/google-or-tools-in-intellij-unsatisfiedlinkerror.

- For Linux ubuntu-20.04 64 -bit

Copy all the files from ubuntu-lib folder into "/user/lib/";
Run "mvn install:install-file -Dfile=./ubuntu-lib/com.google.ortools.jar -DgroupId=com.google.ortools -DartifactId=com-google-ortools -Dversion=7.8.7959 -Dpackaging=jar";
Run "mvn install:install-file -Dfile=./ubuntu-lib/protobuf.jar -DgroupId=com.google.ortools -DartifactId=protobuf -Dversion=7.8.7959 -Dpackaging=jar";
Run mvn exec:java -Dexec.mainClass="Main" to perform simulation.


## Result
With the configuration file coming up with the system, the above command will run simulation on the Manhattan road network with 5000 agents using our search strategy. The resources are the trip records for June 1st, 2016 starting from 8:00am until 10:00pm. The simulation should be finished in a few minutes, and you should get something like the following:
```
average agent search time: 416 seconds 
average resource wait time: 61 seconds 
resource expiration percentage: 0%

average agent cruise time: 345 seconds 
average agent approach time: 52 seconds 
average resource trip time: 673 seconds 
total number of assignments: 236267
total number of abortions: 24
total number of searches: 236267  
```   

=======
# An Effective Fleet Management Strategy for Collaborative Spatio-Temporal Searching (GIS Cup)
>>>>>>> f879424041d851e6feb443f32550db14540a9d5a

