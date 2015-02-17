% SHOWDIVIM - Displays image with diverging colour map
%
% For data to be displayed correctly with a diverging colour map it is
% important that the data values are respected so that the reference value in
% the data is correctly associated with the centre entry of a diverging
% colour map.
%
% In contrast, default display methods typically do not respect data values
% directly and can perform inappropriate offsetting and normalisation of the
% angular data before display and rendering with a colour map.
%
% Usage:  rgbim = showdivim(im, map, refval, fig)
%
% Arguments:
%             im - Image to be displayed.
%            map - Colour map to render the data with.
%         refval - Reference value to be associated with centre point of
%                  diverging colour map.  Defaults to 0.
%            fig - Optional figure number to use. If not specified a new
%                  figure is created. If set to 0 the function runs
%                  'silently' returning rgbim without displaying the image. 
% Returns: 
%          rgbim - The rendered image.
%
% For a list of all diverging colour maps that can be generated by LABMAPLIB
% use: >> labmaplib('div')
%
% See also: SHOW, SHOWANGULARIM, APPLYCOLOURMAP

% Copyright (c) 2014 Peter Kovesi
% Centre for Exploration Targeting
% The University of Western Australia
% peter.kovesi at uwa edu au
% 
% Permission is hereby granted, free of charge, to any person obtaining a copy
% of this software and associated documentation files (the "Software"), to deal
% in the Software without restriction, subject to the following conditions:
% 
% The above copyright notice and this permission notice shall be included in 
% all copies or substantial portions of the Software.
%
% The Software is provided "as is", without warranty of any kind.

% PK October 2014

function rgbim = showdivim(im, map, refval, fig)
    
    if ~exist('refval', 'var'), refval = 0; end
    if ~exist('fig', 'var'), fig = figure; end

    minv = min(im(:));
    maxv = max(im(:));
    
    if refval < minv || refval > maxv
        fprintf('Warning: reference value is outside the range of image values\n');
    end
        
    dr = max([maxv - refval, refval - minv]);
    range = [-dr dr] + refval;
    
    rgbim = applycolourmap(im, map, range);
    
    if fig
        show(rgbim,fig)
    end    
    
    % If function was not called with any output arguments clear rgbim so that
    % it is not printed on the screen.
    if ~nargout
        clear('rgbim')
    end
    