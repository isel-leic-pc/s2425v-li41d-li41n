# Semaphore

A semaphore manages a counter (between 0 and max) with two operations
- Increment, called _release_
- Decrement, called _acquire_, blocks if counter == 0
Invariant: counter >= 0
Synonyms for the counter:
- "permits"
- "units"
Acquire (i.e. decrement) may need to wait until permits/units > 0,
which will happen when another thread calls release (i.e. increment).