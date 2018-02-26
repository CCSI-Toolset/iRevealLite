#!/bin/sh
#please select a compiler, currently use g++
CXX=g++
CFLAGS=-Wall -I.

OBJS=Main.o \
	DesignSite.o \
	Kriging.o \
	LHS.o \
	Matrix.o \
	Simplex.o \
	Util.o \
	YROM.o

.cpp.o:
	$(CXX) $(CFLAGS) -c $<

iReveal: $(OBJS)
	$(CXX) $(CFLAGS) -o iReveal $(OBJS)

$(OBJS): CCSI.h \
	DesignSite.h \
	Kriging.h \
	LHS.h \
	Matrix.h \
	Simplex.h \
	Util.h \
	YROM.h

clean:
	@rm -rf *.o iReveal
