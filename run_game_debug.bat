javac MyBot.java

halite -t -d "300 200" "java -client -agentlib:jdwp=transport=dt_socket,server=y,address=1044,quiet=y MyBot" "java MyBot3"