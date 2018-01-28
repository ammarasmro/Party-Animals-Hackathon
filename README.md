# Party-Animals-Hackathon
A location based social media / chat application

This is a chat application that demonstrates location can be further integrated into our everyday applciations.

Many chat apps exist but we still need to create our own groups and keep sending invites. Many people who send requests to join groups aren't even near the event which can affect the quality of the conversation. This solution uses the technologies available to limit access to conversations to people who are close to the event. 

For the purposes of this hackathon, the Geofencing API wasn't used as it is hard to get a good signal indoors. However, the functionality that lets the device gather locations and put people into conversations nearby is implemented. Data is stored in key, value pairs on Firebase. The pipeline extends beyond this app, K-means clustering was used to gather the 3 clusters of people.
