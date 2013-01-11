/*------------------------------------------------------------------------------
                 PARTICLE SWARM OPTIMIZATION
                  Standard PSO version 2006
--------------------------------------------------------------------------------
Parameters
    S := swarm size
    K := maximum number of particles _informed_ by a given one
    T := topology of the information links
    w := first cognitive/confidence coefficient
    c := second cognitive/confidence coefficient
    R := random distribution of c
    B := rule "to keep the particle in the box"

Equations
    For each particle and each dimension
    v(t+1) = w*v(t) + R(c)*(p(t)-x(t)) + R(c)*(g(t)-x(t))
    x(t+1) = x(t) + v(t+1)
    where
    v(t) := velocity at time t
    x(t) := position at time t
    p(t) := best previous position of the particle
    g(t) := best previous position of the informants of the particle

Default values
    S = 10+2*sqrt(D) where D is the dimension of the search space
    K = 3
    T := randomly modified after each step if there has been no improvement
    w = 1/(2*ln(2))
    c = 0.5 + ln(2)
    R = U(0..c), i.e. uniform distribution on [0, c]
    B := set the position to the min. (max.) value and the velocity to zero
------------------------------------------------------------------------------*/
#include <stdio.h>
#include <math.h>
#include <stdlib.h>

#include "pso.h"

#define	D_max 10 // Max number of dimensions of the search space
#define	S_max 20 // Max swarm size
#define R_max 100 // Max number of runs

//------------------------------------------------------------------------------
//                              PSO()
//
//  Finds global minimum of function in hypercube xmin<=x[i]<=xmax
//  using particle swarm optimization.
//
//  Input parameters:
//      perf           - Function to minimize
//      D              - Search space dimension
//      [xmin,xmax]    - Interval defining the search space
//      eval_max       - Max number of evaluations
//      xBest          - solution
//
//  Output:
//      xBest          - extremum point
//
//      Returns minimal value of function
//------------------------------------------------------------------------------
double PSO(double (*perf)(double*), int D, double x_min, double x_max, int eval_max, double* xBest)
{
    int best;                   // Best of the best position (rank in the swarm)
    int links[S_max] [S_max];   // Information links
    int nb_eval;                // Total number of evaluations
    int S;                      // Swarm size

    double x[S_max][D_max];
    double v[S_max][D_max];
    double xf[S_max];
    double Px[S_max][D_max];
    double Pf[S_max];
    double xmin[D_max], xmax[D_max];

    double c;                   // Second confidence coefficient
    double error;               // Error for a given position
    double error_prev;          // Error after previous iteration
    int g;                      // Rank of the best informant
    int init_links;             // Flag to (re)init or not the information links
    int K;                      // Max number of particles informed by a given one
    double mean_best[R_max];
    double min;                 // Best result through several runs
    int n_exec, n_exec_max;     // Nbs of executions
    double t1, t2;
    double variance;
    double w;                   // First confidence coefficient

    //----------------------------------------------- PROBLEM
    n_exec_max = 1;               // Numbers of runs

    for (int d=0; d<D; d++)  
	{
        xmin[d] = x_min;
        xmax[d] = x_max;
    }

    if(n_exec_max>R_max) 
		n_exec_max=R_max;
    //-----------------------------------------------------  PARAMETERS
	S = 10; 
	if (D>2) S += (int)(2*sqrt(double(D))); 
	if (S>S_max) S=S_max;
    K = 3;
    w = 1/(2*log(2.));
    c = 0.5 + log(2.);

    //----------------------------------------------------- INITIALISATION
    n_exec = 0;

  init:
    n_exec++;

    // Positions and velocities
    for (int s=0; s<S; s++)  
		for (int d=0; d<D; d++)  
		{
			x[s][d] = alea(xmin[d], xmax[d]);
            v[s][d] = (alea(xmin[d], xmax[d]) - x[s][d])/2;
        }

    // First evaluations
    for (int s=0; s<S; s++)
    {
		xf[s] = perf(x[s]);
        Pf[s] = xf[s];
        for (int d=0; d<D; d++) 
			Px[s][d] = x[s][d];      
    }
    nb_eval = S;

    // Find the best
    best = 0;
    for (int s=1; s<S; s++)
        if (Pf[s] < Pf[best]) 
		    best = s;

    error = Pf[best]; // Current min
    if (n_exec==1) min=error;
    error_prev = error; // Previous min

    init_links = 1; // So that information links will be initialized

    //---------------------------------------------- ITERATIONS
  loop:
    if (init_links == 1)
    {
		// Who informs who, at random
        for (int s=0; s<S; s++) 
		{
			for (int m=0; m<S; m++) 
				links[m][s] = 0; // Init to "no link"
			links[s][s] = 1; // Each particle informs itself
        }

        // Other links
        for (int m=0; m<S; m++)  
	    {
            for (int i=0; i<K; i++)  
		    {
			  int s = alea_integer(0, S-1);
			  links[m][s] = 1;
            }
        }
    }

    // The swarm MOVES
    // For each particle ...
    for (int s=0; s<S; s++) 
	{
        // .. find the best informant
        g=s;
        for (int m=0; m<S; m++)  
            if (links[m][s] == 1 && Pf[m]<Pf[g])  
				g = m;
        
        // ... compute the new velocity, and move
        for (int d=0; d<D; d++) 
		{
            v[s][d] = w * v[s][d] + alea(0,c) * (Px[s][d] - x[s][d]);
            v[s][d] = v[s][d] + alea(0,c) * (Px[g][d] - x[s][d]);
            x[s][d] = x[s][d] + v[s][d];
        }

        // ... interval confinement (keep in the box)
        for (int d=0; d<D; d++)  
		{
            if (x[s][d] < xmin[d])  
			{
                x[s][d] = xmin[d];
                v[s][d] = 0;
            }
            if (x[s][d] > xmax[d])  
			{
                x[s][d] = xmax[d];
                v[s][d] = 0;
            }
        }

        // ... evaluate the new position
        xf[s] = perf(x[s]);

        // ... update the best previous position
        if (xf[s] < Pf[s])   
		{
            Pf[s]=xf[s];
            for (int d=0; d<D; d++) 
                Px[s][d]=x[s][d];            

            // ... update the best of the bests
            if (Pf[s] < Pf[best]) 
				best=s;
        }
    }
    nb_eval += S;

    // Check if finished
    // If no improvement, information links will be reinitialized
    error = Pf[best];
    if (error >= error_prev) 
		init_links = 1;
    else 
		init_links = 0;
    error_prev = error;

    if (nb_eval < eval_max) 
		goto loop;

    if (error < min) 
		min = error;

    if (n_exec < n_exec_max) 
		goto init;

  end:
    for (int d=0; d<D; d++)  
		xBest[d] = Px[best][d];

    return min;
}

//-----------------------------------------------------------
// random number (uniform distribution) in [a b]
//-----------------------------------------------------------
double alea(double a, double b)
{
    double r = (double)rand();
    r = r / RAND_MAX;
    return a + r * ( b - a );
}

//-----------------------------------------------------------
// Integer random number in [a b]
//-----------------------------------------------------------
int alea_integer(int a, int b)
{
    double r = alea(0, 1);
    int ir = (int)(a + r * ( b + 1 - a ));
    if (ir > b) 
		ir = b;
    return ir;
}

