
// Copyright (C) 2012 thuxnder@dexlabs.org
// Based on the x86 version of pleed@dexlabs.org
//
// Licensed under the Apache License, Version 2.0 (the 'License');
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an 'AS IS' BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

#include <sched.h>
#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include <stdint.h>

#include <signal.h>
#include "uthash.h"
#include "bt_detect.h"
// #include <android/log.h>

volatile int numbers_cnt=0;			// counter with each distinct number we saw

volatile int die = 0, secs = 0;

void do_die(int signum)
{
		die=1;
}

/* struct to store the numbers along with a counter
   keeping the time we have encounter them */
typedef struct {
		int number;					// the number to be incremented during execution
		UT_hash_handle hh;	// make this structure a hashtable
} statistic;

statistic *stats = NULL;

/* add an item to the hash table */
void add_number(statistic *s) {
	HASH_ADD_INT ( stats, number, s);
}

/* Looking up an item in the hash table */
statistic *find_number(int number) {
	statistic *s;

	HASH_FIND_INT( stats, &number, s);
	return s;
}

/* Return the count of items in the hash table*/
unsigned int count_numbers () {
	unsigned int count = HASH_COUNT(stats);
	return count;
}


volatile uint32_t global_value = 32;
#define PRINT "value: 0x%x\n"

void* thread2(void * data){
	int i = 0, numbers = 0;
	statistic *tmp, *s;
	while (!die) {
		printf("2: %d\n", i++);

		/////////////////////
		// print to LOGCAT
		// char buffer[10];
		// snprintf(buffer, 10, "%d", global_value);
		// __android_log_write(ANDROID_LOG_ERROR, "[[BT]]", buffer);
		/////////////////////

		tmp = find_number(global_value);

		if (tmp == NULL){
			// this is the first time we see this number,
			// let's put it in the hash table
			s = malloc(sizeof(statistic));
			s->number=global_value;
			add_number(s);

			// we saw a new distinct number
			numbers_cnt += 1;
		}
	}
}

void* thread1(void * data){
	int i =0;
	while (!die) {
		printf("1: %d\n", i++);
		__asm__ __volatile__ ("mov r0, %[global];"
		                      "mov r1, #1;"
		                      "add r1, r1, #1;" "str r1, [r0];"
		                      "add r1, r1, #1;" "str r1, [r0];"
		                      "add r1, r1, #1;" "str r1, [r0];"
		                      "add r1, r1, #1;" "str r1, [r0];"
		                      "add r1, r1, #1;" "str r1, [r0];"
		                      "add r1, r1, #1;" "str r1, [r0];"
		                      "add r1, r1, #1;" "str r1, [r0];"
		                      "add r1, r1, #1;" "str r1, [r0];"
		                      "add r1, r1, #1;" "str r1, [r0];"
		                      "add r1, r1, #1;" "str r1, [r0];"
		                      "add r1, r1, #1;" "str r1, [r0];"
		                      "add r1, r1, #1;" "str r1, [r0];"
		                      "add r1, r1, #1;" "str r1, [r0];"
		                      "add r1, r1, #1;" "str r1, [r0];"
		                      "add r1, r1, #1;" "str r1, [r0];"
		                      "add r1, r1, #1;" "str r1, [r0];"
		                      "add r1, r1, #1;" "str r1, [r0];"
		                      "add r1, r1, #1;" "str r1, [r0];"
		                      "add r1, r1, #1;" "str r1, [r0];"
		                      "add r1, r1, #1;" "str r1, [r0];"
		                      "add r1, r1, #1;" "str r1, [r0];"
		                      "add r1, r1, #1;" "str r1, [r0];"
		                      "add r1, r1, #1;" "str r1, [r0];"
		                      "add r1, r1, #1;" "str r1, [r0];"
		                      "add r1, r1, #1;" "str r1, [r0];"
		                      "add r1, r1, #1;" "str r1, [r0];"
		                      "add r1, r1, #1;" "str r1, [r0];"
		                      "add r1, r1, #1;" "str r1, [r0];"
		                      "add r1, r1, #1;" "str r1, [r0];"
		                      "add r1, r1, #1;" "str r1, [r0];"
		                      "add r1, r1, #1;" "str r1, [r0];"
													:
		                      :[global] "r" (&global_value)
		                      );
	}
}

/* Returns 1 if is in emulator environment, 0 otherwise */
#JNIEXPORT jint JNICALL Java_com_btdetector_BinaryTranslationDetection_is_1bt
//#JNIEXPORT jint JNICALL Java_install_app_BinaryTranslationDetection_is_1bt
  (JNIEnv *je, jclass jc)
{
	pthread_t thread_data1, thread_data2;
	int status[2];
	int *ret;

	signal(SIGALRM, do_die);

	alarm(5);

	status[0] = pthread_create(&thread_data2, NULL, thread2, NULL);
	if(status[0])
		perror("pthread_create()");

	status[1] = pthread_create(&thread_data1, NULL, thread1, NULL);
	if(status[1])
		perror("pthread_create()");


	pthread_join(thread_data2, NULL); //, (void **)&(ret));
	pthread_join(thread_data1, NULL); //, (void **)&(ret));

	return (numbers_cnt == 1) ? 1 : 0;
	//return (numbers_cnt);
	//return (count_numbers());
}
