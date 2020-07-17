# SREDX

sredx (pronounced shred-x) is a tool that can be used to help extract developer activities from Atlassian's JIRA and Git in order to facilitate report writing.

Developer activities are extracted, arranged by time and exported to a .csv so that they can be easily imported into a spreadsheet tool.

## Building

Requires Java 8 and Maven 3.x+:
```
mvn clean package
```

## Running

Create a file called `sred.yaml` with contents similar to:
```
---
jira:
   url: https://myjira.myorg.com
   username: someuser
   password: somepassword
users:
  - name: Jesse White
    jira-user: not-jesse-white
    email:
      - not-jesse@opennms.org
  - name: Other User
    jira-user: other
    email:
      - other.user@myorg.com
projects:
  - name: alec
    code: ALEC
    start-date: 2019-01-01T00:00:00Z
    end-date: 2019-05-01T00:00:00Z
    git-repositories:
      - /usr/src/git/opennms/.git
```

Run the application with:
```
java -jar target/sredx-1.0.0-SNAPSHOT.jar -f sred.yaml 
```

A file called `out.csv` will be created in the current working directory that contains a summary of all the activities found.

## JIRA

Tested against:
```
Version	8.8.0
Build Number	808000
Build Date	Thu Mar 19 00:00:00 UTC 2020
Build Revision	e2c7e59ae165efc6ad6b529150e24d091b9947bf
```

