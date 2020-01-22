# Smoke Signals
This is a decentralized social media platform. It runs on the ipfs network and is no longer in development, but is not finished. We have a lot working in a command line format. If someone wants to pick it up and try to run with it we have plenty of todo statements to give you an idea of where things are. At some point I plan to go through and comment the code properly, but right now it all the functions have JavaDoc and sparse comments in addion to that. If you have questions about it send me an email at calebmorton98@gmail.com. I will try to get to you quickly.
# To run
Install go-ipfs and than run:
```
ipfs daemon --enable-pubsub-experiment
```
```
mvn clean install
```
```
cd target && java -cp SmokeSignalsJava-0.0.1.jar:dependency com.Smoke.Signals.main
```
