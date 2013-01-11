#ifndef psoH
#define psoH

double PSO(double (*perf)(double*), int D, double x_min, double x_max, int eval_max, double* xBest);
double alea(double a, double b);
int alea_integer(int a, int b);
double findMax2(double (*func)(double*), double xmin, double xmax, double step, double* x);

#endif
