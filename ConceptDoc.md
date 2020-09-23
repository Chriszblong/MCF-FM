# GISCUP 2020 Concept Design
## Background
In 2020 GISCUP will build upon GISCUP 2019.
Specifically GISCUP 2020 extends the original, with several new twists:

* **Management of an entire fleet of taxis**,
i.e. a contestant-provided **`Fleet Manager`** that determines an empty taxi's cruising path,
assigns customers to taxi's, directs the taxi to pick-up and to drop-off.
The manager will control the agent at a more granular level than in 2019.
* **Dynamic Travel Times**. The travel time through each segment can vary
probabilistically as a function of time-of-day
and is determined by a traffic model.
* Failures. Taxis may fail at any time. _**UNDER DISCUSSION**_

For more details, see [GISCUP 2020 Problem Definition](https://docs.google.com/document/d/e/2PACX-1vQ6PL6krQtLjtWs8pI3UKI_NhNuFr_Ecl_Kfk77Yt3ZLzrf2lWt6A1UUCgAbf3JMgnXR9VhfWXJCtab/pub)

This document outlines GISCUP 2020's conceptual differences from GISCUP 2019.
We do not intend to cover all of 2019's functionality but to
supply enough details to highlight the key differences.

In the code and here, the term agent is synonymous with taxes; resource, with customers.
Here, we use the synonyms interchangeably.

## Simulation in GISCUP 2019

GISCUP 2019's simulator is a standard event simulator that maintains a priority
queue of future events ordered by increasing time.
Each simulator step retrieves the next scheduled event, advances simulated time to its
scheduled time, and triggers the event.
The simulation consists of two varieties of events implemented by the classes
`AgentEvent` and `ResourceEvent`.
An `AgentEvent` implements its associated `Agent`'s behavior;
a `ResourceEvent` implements a customer's behavior.

### Types of `AgentEvent`
Two `AgentEvent` types models the behavior of an agent. Each AgentEvent is
associated with an agent, represented by an instance of a
 contestant-supplied subclass of `BaseAgent`. The agent performs
route planning.

1. **`DROPPING_OFF`** This represents the event of droppping off
a resource at its destination, and then searching for another resource to
service. If the search fails the agent then goes into cruising mode.
Because searching for a resource should be the first thing an empty agent
should do, every agent begins with a `DROPPING_OFF` event.

2. **`INTERSECTION_REACHED`**
This represents the event of reaching a road intersection.
An agent's cruising path is manifested by a sequence of `INTERSECTION_REACHED` events,
one for each intersection in its path.
Interrupting an agent from cruising to pick up a resource is implemented by removing
an agent's scheduled `INTERSECTION_REACHED` event, and scheduling a
`DROPPING_OFF` event for the agent.

### States of `ResourceEvent`
Two `ResourceEvent` types are used to model a resource or a customer.
Not there is no corresponding `Resource` class.

1. `BECOME_AVAILABLE` represents the event of a resource looking for a ride.
The handler searches for the *closest* cruising agent, one that can reach the
pick up point the soonest.
Once identified the agent is interrrupted by descheduling its next
`INTERSECTION_REACHED` event and scheduling a `DROPPING_OFF` event

    **Note:** When the corresponding `DROPPING_OFF` event is handled,
    it searches for the nearest resource, i.e. the one above.

2. `EXPIRED` represents a resource in which all agents cannot reach it
before its maxiumum life time expires.
Contestants are penalized for expired resources.

### Representation of an Agent
An agent is represented by two classes `AgentEvent` and `Agent`.
The event class models the behavior.
The contestants supplies their own `Agent` class by deriving the
abstract class `BaseAgent`.
The supplied class plans cruising roues to maximize the likelihood of
an available agent to serve a resource with minimal waiting time.

## Simulation in GISCUP 2020

### Fleet Management

In 2020 we introduce the concept of fleet management as represented by
a contestant supplied Fleet Manager class, derived from the abstract
class `BaseFleetManager`.
This class provides route planning and resource assignment.
In 2020 the fleet manager will have to plan all routes to take into
account dynamic travel time.
The fleet manager can reconsiders each agent's route at each intersection.

### More Granular Route Simulation

In 2019 simulation of the in-service agent is lumped, i.e.
the route from pick-up to drop-off is not fully simulated.
Instead the simulator just jumps time forward for the agent
from pick-up to drop-off, where time is the travel time of
the shortest path between pick-up and drop-off.
In 2020 simulation of all travels will be simulated road segment 
by road segment to
implement dynamic travel time.

### Proposals for Implementation

#### Remove `BaseAgent`
Much of `BaseAgent`'s functionality will have been supplanted by
`BaseFleetManager`.
We will identify an agent to the Flee tManager by index.
Most of an agent's behavior will be modeled by the Fleet Manger.

#### Add more refined types of events in `AgentEvent`

Introduced new event types will represent the following cases:

* `TRAVELLING` Whether an agent is empty or not, its travels will
be simulated. When an empty agent reaches the road segment on which its assigned
resource is to be picked-up, its next event will be of type `PICK_UP`.
Or when an non-empty agent reach the road segment
on which its associating resource is to be dropped-off,
its next event will be of type `DROP_OFF`

* `PICK_UP` The agent travels to its designated pick up point on the segment
and *picks up* the resource, and its next event will be of type `TRAVELLING`

* `DROP_OFF` The agent travels to its designated rop off point on the segment
and *drops off* the resource, and its next event will be of type `TRAVELLING`

#### Add corresponding event types for `ResourceEvent`

* `AVAILABLE` To handle this event call the Fleet Manager to assign the
resource to an agent. It will repeated call for assignment at some set
interval (maybe 10 minutes) until assigned.

* `PICK_UP` The resource is waiting to be picked up.

* `DROP_OFF` The resource is dropped off, compute a service time, and is
removed from the system.

#### Dynamic Traffic Speeds
The travel speed at a road segment will be updated every Q minutes (e.g., N=15) during a simulation. The travel speed is computed based on the road segment's speed limit and the TLC Trip Record data to reflect the traffic condition over the time of a day. The calibration goes as follows.

1. Compute the average trip duration of all trips recorded in the TLC Trip Record data that fall into the current Q-minute interval; call it the `TLC_average_trip_duration`.
2. For each trip, compute the shortest travel time from the pickup location of the trip to the dropoff location using speed limits.
3. Compute the average shortest travel time of all trips; call it the `map_average_trip_duration`.
4. For each road segment, `travel_speed = speed_limit * ((map_average_trip_duration)/(TLC_average_trip_duration))`.

In other words, we adjust the travel speeds so that the average trip time produced by COMSET is consistent with tachat by the real data.

## Implementation Plan

1. Write Unit Tests for existing GISCUP 2019. This is an ideal way to but we might not have time for this.
1. Move all planning into a new `BaseFleetManager` class, and supply fleet managers that models current
random walk and random destination agent.
1. Introduce `TRAVELLING` event type to simulate cruising.
1. Introduce other evet types.
