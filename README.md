# AST Tuner

An Android instrument tuning app. No ads, no premium features. Fully customizable tuning.

The app is basically feature complete, but could probably use some polish and optimization. Feel free to make suggestions or pull requests if you know more about android developement than I do (which is a low bar)

How it works:
The app calculates the autocorrelation of the input signal and detects the highest peak. The peak can be used to calculate the fundamental frequency of the current note (see the reference below). 
To see what the app sees, you can swipe left on the frequency display. It will display the autocorrelation, the detected peaks, and the current frequency estimate.

# References (Signal Processing Theory)

> Smith, J.O. Spectral Audio Signal Processing,
> http://ccrma.stanford.edu/~jos/sasp/, online book,
> 2011 edition,
> accessed 2024-06-29.

