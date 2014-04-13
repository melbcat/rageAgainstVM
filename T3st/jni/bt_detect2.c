#include <stdio.h>
#include <sys/mman.h>
#include <stdint.h>
#include <time.h>
#include <string.h>

#include "bt_detect2.h"

#define CLOCKTYPE CLOCK_MONOTONIC

char switch_str[21]="";
char expected[21]="12121212121212121212";


int total=0;

/* The first test function. The emitted code will start off
   with a branch to this function.
*/
void f1(void)
{
    // printf(" f1 \n");
		double a, b, c, d, e, f, g, h, i;
		a = 4;
		b = 23;
		c = 7;
		d = 19;
		e = 122;
		f = 32;
		h = 89;
		i = 66;

		a = a * a * b / f;
		b = a + c + d * e / i;
		c*= e * b * a / b;
		d*= h * c + i + h * i;
		f+= d * c * a * b;
		h*= f/a * i - c;
		i*= h / 3 + e / 2;

		total += i;
		strcat(switch_str, "1");
}

/* The second test function. The emitted code will be modified to
   call thisfunction.
*/
void f2(void)
{
    // printf(" f2 \n");
		double a, b, c, d, e, f, g, h, i;
		a = 34;
		b = 2;
		c = 74;
		d = 9;
		e = 222;
		f = 3;
		h = 99;
		i = 66;

		a = a * a * b / f + e;
		b = a + c * d * e * i + h;
		c*= i * e * b * a / b;
		d*= h + i + h / f;
		f+= d * f * a * b;
		h*= e/a * i - c * f;
		i*= h / 3 + e * 2;

		total += i;
		strcat(switch_str, "2");
}

/* Append an instruction to a buffer
*/
void emit(uint32_t ** code, uint32_t ins)
{
    **code = ins;
    (*code)++;
}

/* patch the code
*/
void patch_code_ARMv5(uint32_t ** caret, uint32_t ** patch, test_func_t patch_func)
{
	*caret = *patch;
  emit(caret, MOVW(ip, (uint32_t)patch_func));
  emit(caret, MOVT(ip, (uint32_t)patch_func));
}

void write_code(uint32_t ** caret, uint32_t ** code, uint32_t ** patch, test_func_t patch_func)
{
    *caret = *code;
    emit(caret, PUSH(rmask(4) | rmask(lr)));
    *patch = *caret;  // Store the location of the code that we want to patch.

    emit(caret, MOV(ip, OP2_BYTE0 | (((uint32_t)patch_func >>  0) & 0xff)));
    emit(caret, ORR(ip, OP2_BYTE1 | (((uint32_t)patch_func >>  8) & 0xff)));
    emit(caret, ORR(ip, OP2_BYTE2 | (((uint32_t)patch_func >> 16) & 0xff)));
    emit(caret, ORR(ip, OP2_BYTE3 | (((uint32_t)patch_func >> 24) & 0xff)));
    emit(caret, BLX(ip));
    emit(caret, POP(rmask(4) | rmask(pc)));
}

/* patch the code
*/
void patch_code_other(uint32_t ** caret, uint32_t ** patch, test_func_t patch_func)
{
	*caret = *patch;
	emit(caret, MOV(ip, OP2_BYTE0 | (((uint32_t)patch_func >>  0) & 0xff)));
	emit(caret, ORR(ip, OP2_BYTE1 | (((uint32_t)patch_func >>  8) & 0xff)));
	emit(caret, ORR(ip, OP2_BYTE2 | (((uint32_t)patch_func >> 16) & 0xff)));
	emit(caret, ORR(ip, OP2_BYTE3 | (((uint32_t)patch_func >> 24) & 0xff)));
}



JNIEXPORT jint JNICALL Java_com_btdetector2_NativeWrapper_is_1synch_1proc_1switch
  (JNIEnv * je, jclass jc)
{
    // a function pointer to our test code
    test_func_t     test_func;

    // a pointer to some code that we will patch
    uint32_t *  patch;

    // a useful swap pointer
    uint32_t *  caret;

		int i=0;
		double sum=0;
		struct timespec tsi, tsf;
		double elaps_ns, elaps_s;


    // we need some memory for our run-time-generated function.
    // w+x rights in order to be able to patch the code
    // 16 * 4,  // Space for 16 instructions. (More than enough.)
    uint32_t *  code = mmap(
            NULL,
            16 * 4,  // Space for 64 instructions. (More than enough.)
            PROT_READ | PROT_WRITE | PROT_EXEC,
            MAP_PRIVATE | MAP_ANONYMOUS,
            -1,
            0);

    if (code == MAP_FAILED) {
        printf("Could not mmap a memory buffer with the proper permissions.\n");
        return -1;
    }

    // Set test_func to point to the writable code block
    test_func = (test_func_t) code;

    // initialize test_func with f1 code
		write_code(&caret, &code, &patch, &f2);

    // Synchronize the cache.
    // __clear_cache((char*)code, (char*)caret);

		sum=0;
		for (i=0; i<10; i++) {

			// patch code with f1, synch I-cache and run the function
			patch_code_other(&caret, &patch, &f1);
			test_func();

			// patch code with f2, synch I-cache and run the function
			patch_code_other(&caret, &patch, &f2);
			test_func();
		}

	if (strcmp(switch_str, expected) == 0) {
		// printf("\n is Emulator! \n");
		return 1;
	}
	else {
		// printf("\n is Device! \n");
		return 0;
	}
}

