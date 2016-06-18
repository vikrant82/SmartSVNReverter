# SmartSVNReverter

Challenge - I have a stable branch accompanied by nightly tests. Person A checks in something unstable. Day 1 nightly detects it. On day 2, person B wants to check-in. Well he can't really know his failures as branch is already unstable. So this is where this utility steps in. It reverts all checkins made before last run upto last to last run. So, that it tests last window's check-ins on base. Helpful in running daily tests where many people are working on same branch. Typically bug fix branches.
<br />
<br />

For e.g. Hook in this to your confidence run and the starting SVN revision is 100
<br />

Day 1 - PersonA checked in F1.java, F2.java - 101<br />
Day 1 - PersonA checked in F2.java - 102<br />
Day 1 - PersonB checked in F3.java - 103<br />
----- Confidence test runs ----- #1<br />
    - Nothing special, all good - You have tested all Day 1 changes in isolation over version 100<br />
Day 2 - PersonB checked in F1.java - 104<br />
Day 2 - PersonB checked in F3.java - 105<br />
---- Confidence test runs ------ #2<br />
    --> Reverts check-ins between start and confidence run #1 - 101, 102 and 103<br />
    --> Commits these reverts - 106<br />
    --> Runs confidence - This is result of only Day 2 check-ins in isolation over version 100.<br />
Day 3 - Nothing checked in.<br />
    ---- Confidence runs ------ #3<br />
    --> Reverts between #1 and #2 - 104, 105<br />
    --> Commits these reverts - 107<br />
    --> Runs confidence - basically same as base.<br />
<br />
How to configure..<br />
<br />
1. Do a manual checkout SVN project for a base revision number at start of sprint.<br />
3. Make sure following command is run before your tests are run.<br />
<br />
    "java -jar SVNReverter-0.0.1-SNAPSHOT-jar-with-dependencies.jar -w /home/kx/SVNCheckout -u username -p password"<br />
            -w is optional if you are running in current checkout directory.<br />
<br />
<br />
