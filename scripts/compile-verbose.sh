#!/bin/bash
cd backend
./mvnw clean compile -X | grep -A5 -B5 "incompatible types.*Tags.*double"