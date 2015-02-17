% RELIEF  Generates relief shaded image
%
% Usage:  shadeim = relief(im, azimuth, elevation, dx, rgbim)
%
% Arguments:  im - Image/heightmap to be relief shaded.
%        azimuth - Of light direction in degrees. Zero azimuth points 
%                  upwards and increases clockwise. Defaults to 45.
%      elevation - Of light direction in degrees. Defaults to 45.
%      gradscale - Scaling to apply to the surface gradients.  If the shading
%                  is excessive decrease the scaling. Try successive doubling
%                  or halving to find a good value. 
%          rgbim - Optional RGB image to which the shading pattern derived
%                  from 'im' is applied.   Alternatively, rgbim can be a Nx3
%                  RGB colourmap which is applied to the input
%                  image/heightmap in order to obtain a RGB image to which
%                  the shading pattern is applied.
%
% This function generates a relief shaded image.  For interactive relief
% shading use IRELIEF.  IRELIEF reports the azimuth, elevation and gradient
% scaling values that can then be reused on this function.
%
% Lambertian shading is used to form the relief image.  This obtained from the
% cosine of the angle between the surface normal and light direction.  Note that
% shadows are ignored.  Thus a small feature that might otherwise be in the
% shadow of a nearby large structure is rendered as if the large feature was not
% there.
%
% See also: IRELIEF, APPLYCOLOURMAP

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

% April 2014  

function  shadeim = relief(im, az, el, gradscale, rgbim)

    [rows, cols, chan] = size(im);
    assert(chan==1)
    
    if ~exist('az', 'var'),    az = 45;  end
    if ~exist('el', 'var'),    el = 45;  end
    if ~exist('gradscale', 'var'),    gradscale = 1;     end
    
    if exist('rgbim', 'var') 
        [rr, cc, ch] = size(rgbim);
        if cc == 3 && ch == 1  % Assume this is a colourmap that is to be
                               % applied to the image/heightmap 
            rgbim = applycolourmap(im, rgbim);
        
        elseif ~isempty(rgbim) % Check its size
            if rows ~= rr || cols ~= cc || ch ~= 3
               error('Sizes of im and rgbim are not compatible'); 
            end
        end
    else  % No image supplied
        rgbim = []; 
    end
    

    % Obtain surface normals of im
    loggrad = 'lin';
    [n1, n2, n3] = surfacenormals(im, gradscale, loggrad);
    
    % Convert azimuth and elevation to a lighting direction vector.  Note that
    % the vector is constructed so that an azimuth of 0 points upwards and
    % increases clockwise.
    az = az/180*pi;
    el = el/180*pi;
    I = [cos(el)*sin(az), cos(el)*cos(az), sin(el)];
    I = I./norm(I); % Ensure I is a unit vector     
    
    % Generate Lambertian shading via the dot product between surface normals
    % and the light direction.  Note that the product with n2 is negated to
    % account for the image +ve y increasing downwards.
    shading = I(1)*n1 - I(2)*n2 + I(3)*n3; 

    % Remove -ve shading values which are generated by surface normals pointing
    % away from the light source.
    shading(shading < 0) = 0;
    
    % If no RGB image has been supplied just return the raw shading image
    if isempty(rgbim)
        shadeim = shading;
        
    else  %  Apply shading to the RGB image supplied
        shadeim = zeros(size(rgbim));
        
        for n = 1:3
            shadeim(:,:,n)  = rgbim(:,:,n).*shading;
        end
    end
    
    % ** Resolve issue with RGB image being double or uint8
    
    
%---------------------------------------------------------------------------
% Compute image/heightmap surface normals

function [n1, n2, n3] = surfacenormals(im, gradscale, loggrad)
    
    % Compute partial derivatives of z.
    % p = dz/dx, q = dz/dy

    [p,q] = gradient(im);  
    p = p*gradscale;
    q = q*gradscale;
    
    % If specified take logs of gradient.
    % Note that taking the log of the surface gradients will produce a surface
    % that is not integrable (probably only of theoretical interest)
    if strcmpi(loggrad, 'log')
        p = sign(p).*log1p(abs(p));
        q = sign(q).*log1p(abs(q));
    elseif strcmpi(loggrad, 'loglog')
        p = sign(p).*log1p(log1p(abs(p)));
        q = sign(q).*log1p(log1p(abs(q)));
    elseif strcmpi(loggrad, 'logloglog')
        p = sign(p).*log1p(log1p(log1p(abs(p))));
        q = sign(q).*log1p(log1p(log1p(abs(q))));
    end
    
    % Generate surface unit normal vectors. Components stored in n1, n2
    % and n3 
    mag = sqrt(1 + p.^2 + q.^2);
    n1 = -p./mag;
    n2 = -q./mag;
    n3 =  1./mag;

    