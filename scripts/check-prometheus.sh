#!/bin/bash
cd backend
./mvnw dependency:tree | grep -i prometheus