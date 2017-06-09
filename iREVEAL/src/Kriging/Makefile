#!/bin/sh

CXX=g++
CFLAGS=-Wall -I. -mno-cygwin
OBJS=Main.o \
	DesignSite.o \
	Kriging.o \
	Matrix.o \
	Simplex.o \
	Util.o \
	YROM.o
	
.cpp.o:
	$(CXX) $(CFLAGS) -c $<
rom: $(OBJS)
	$(CXX) $(CFLAGS) -o rom $(OBJS)
$(OBJS): CCSI.h \
	DesignSite.h \
	Kriging.h \
	Matrix.h \
	Simplex.h \
	Util.h \
	YROM.h

clean:
	rm *.o rom
